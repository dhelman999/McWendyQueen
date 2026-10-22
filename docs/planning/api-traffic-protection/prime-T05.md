# Prime: T05 idle-bucket sweep and tracked-key cap

Read `prime-shared.md` first. **Run this after T04 lands.** `RateLimitConfig` is
a shared file: T04 creates it and this ticket edits it. See "Shared files and run
order".

## Current behavior

- The app has no `@EnableScheduling` and no `@Scheduled` method. `OrderScheduler` is
  a Kafka listener despite its name. Turning scheduling on app-wide would be inert
  today, but the ticket wants that decision recorded rather than made as a side
  effect.
- `BucketStore` (T02) is a `ConcurrentHashMap<String, ClientBuckets>` keyed by client
  address. An entry is swept only when **all** of its route-group buckets are full.
  `max-tracked-keys` counts client addresses.
- Edits to `RateLimitConfig`: add the sweeper bean, which uses the existing
  `rateLimitClock` bean and `sweep-interval`, and pass `max-tracked-keys` into the
  store's `@Bean` method. Do not add `@EnableConfigurationProperties`; see
  "Properties registration".

## Recommended approach

- Keep scheduling local. Run `BucketSweeper` on its own single-thread
  `ScheduledExecutorService`, created from a named daemon `ThreadFactory`, and shut
  the executor down in `@PreDestroy` or `close()`. That avoids `@EnableScheduling`,
  and shutdown can be tested.
- Atomic removal: for each key, call
  `map.computeIfPresent(key, (k, v) -> v.isFullAt(now) ? null : v)`. A remapping
  function that returns `null` removes the entry inside the same lock that
  `tryConsume` takes, so the sweep cannot race a consume. Iterating `keySet()` is
  weakly consistent, which is fine here.
- Cap: when `tryConsume` sees a new key while `size() >= maxTrackedKeys`, it allows
  the request without tracking the key. Gate the WARN with an `AtomicLong`
  holding the last-warn nanos, updated by CAS, so at most one WARN fires per
  `sweep-interval`. Document that the cap is soft by up to the thread count, since
  `size()` is checked before insert. That is acceptable for a backstop.

## Traps

- "Full" means full **after** refilling to the sweep time, not the stored token
  count. Reuse T02's "is full at time t" check. Do not write a second copy.
- The test yml sets `max-tracked-keys: 1000`. Do not lower it there, because that
  would change every test context. Build a small-cap store directly in
  `BucketSweeperTest`.
- Tests must not leak the executor thread. Build the sweeper directly, then call
  `sweepOnce(now)` and keep the timer out of the logic tests. Add one test that
  starts the timer and asserts the thread is gone after `close()`.

## Tests that already cover the area

`BucketStoreConcurrencyTest` (T02) has to stay green after the cap check is added to
`tryConsume`.

## Suggested first edit (tests first)

`BucketSweeperTest` on the fake clock. Cover four cases: full buckets are swept, a
part-drained bucket is kept, a swept key comes back full, and the cap allows the
request plus a single WARN, checked with `OutputCaptureExtension`. Then add one race
test: a sweeper thread and a consumer thread hammer one key, and no consume is lost.
