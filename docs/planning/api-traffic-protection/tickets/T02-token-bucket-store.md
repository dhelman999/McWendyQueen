# T02: Token bucket and bucket store

- Slug: `api-traffic-protection`
- Size: M
- Depends on: none
- PRD: `docs/planning/api-traffic-protection/prd.md`
- Spec: `docs/planning/api-traffic-protection/spec.md`

## Goal

A pure-Java token bucket and a thread-safe store of buckets keyed by client
address, holding one bucket per route group (see Amendment), with no Spring and no
servlet code. It decides allow or deny
and computes the retry time. This is the roughly 150 lines of concurrency and refill
code the spec says we own and must test well. It sits behind an interface so
Bucket4j (the v2 path) could replace the inside without the filter changing.

## Amendment (2026-10-21, owner)

This amends the accepted spec's data model. The spec keyed the store by (client
address, route group). The store is now keyed by **client address alone**. Each
entry holds one bucket per `RouteGroup` (`GENERAL`, `ORDERS`).

Why: the AC below requires an all-or-nothing consume from two buckets. Two
`compute()` calls on two map keys cannot be atomic together, while one `compute()`
on one key is. A side effect is that T05's sweep removes one entry per client.

Consequences: `RouteGroup` is created here, not in T04. The cap in
`max-tracked-keys` (T05) counts client addresses, not (client, group) pairs. The
pinned names are in `prime-shared.md`.

## Amendment 2 (2026-10-21, owner)

Refill is **`refillTokens` per `refillPeriod`** (a `Duration`), not an integer rate
per second. `BucketLimits` is `(long capacity, long refillTokens, Duration
refillPeriod)`.

Why: with integer tokens per second, the slowest possible refill is 1 per second,
and `Retry-After` is always 1. That makes the rounding AC below meaningless and
stops `orders` from being tighter than 1 per second. With a period, `orders` can be,
for example, 5 per minute, and `Retry-After` takes real values.

## Acceptance criteria

- [ ] New package `com.mcwendyqueen.ratelimit`.
- [ ] A bucket has capacity (the burst) and a refill of `refillTokens` per
      `refillPeriod` (Amendment 2). It starts full.
- [ ] Time comes from an injectable nanosecond clock (`System.nanoTime()` in
      production). Integer arithmetic only: no wall clock, no `double`.
- [ ] `tryConsume(key)` returns either allowed, or denied with a retry time in
      whole seconds, rounded up and at least 1.
- [ ] The store is a `ConcurrentHashMap` keyed by client address, and each key is
      updated atomically with `compute()`. No bucket state is read or written
      outside it.
- [ ] Refill never raises tokens above capacity, however long the bucket was idle.
- [ ] Consuming from two buckets at once (orders, then general - needed by T04) is
      one call that takes from neither if either is empty. T04 owns whether orders
      also count against `general`; this ticket only makes it possible.

## Definition of done

- Acceptance criteria all checked.
- Tests cover: a full burst then a deny; refill after a known elapsed time;
  `Retry-After` rounding at, just below and just above a boundary; the capacity cap
  after a long idle; and `nanoTime` values that wrap past `Long.MAX_VALUE`.
- Concurrency test: N threads on one key with capacity C and a frozen clock get
  exactly C allows, repeated enough times to be meaningful.
- No new dependency in `pom.xml`.
- Change reviewed through skill `triple-check`.

## Likely files

- `src/main/java/com/mcwendyqueen/ratelimit/TokenBucket.java` (new)
- `src/main/java/com/mcwendyqueen/ratelimit/BucketStore.java` (new)
- `src/main/java/com/mcwendyqueen/ratelimit/RateLimitDecision.java` (new)
- `src/main/java/com/mcwendyqueen/ratelimit/RouteGroup.java` (new, moved from T04)
- `src/test/java/com/mcwendyqueen/ratelimit/TokenBucketTest.java` (new)
- `src/test/java/com/mcwendyqueen/ratelimit/BucketStoreConcurrencyTest.java` (new)

## Validation (do this before calling the ticket done)

Failing tests are already written against stubs (2026-10-21): `TokenBucketTest`
(16) and `BucketStoreConcurrencyTest` (3), all red on assertions. Implement until
they are green; do not edit their assertions. If one looks wrong, raise it rather
than changing it.

- Unit: `./mvnw test -Dtest=TokenBucketTest,BucketStoreConcurrencyTest`
- Full suite: `./mvnw test`
- Manual: none. This ticket has no HTTP surface.

Write failing tests first when implementing this ticket (skill `validation-tdd`). Ship still goes through skill `triple-check`.
