# Prime: T02 token bucket and bucket store

Read `prime-shared.md` first. It pins `RouteGroup`, `BucketLimits`, `BucketStore`
and `RateLimitDecision`. The store is keyed by client address alone, with one bucket
per route group. That is an owner decision and spec amendment (2026-10-21),
recorded in the T02 ticket under "Amendment".

## Current behavior

Nothing exists yet. `com.mcwendyqueen.ratelimit` is a new package with no Spring
and no servlet imports. Two pieces of in-repo prior art for concurrent maps are not
good models: the `ConcurrentHashMap` in `OrderServiceImpl`, and the unsynchronized
`LinkedHashMap` in `ResiliantOrderPolicyEligibilityClient` (known issue 2). Do not
copy either pattern.

## Design notes (recommended)

- `TokenBucket` is an immutable value (`scaledTokens`, `lastRefillNanos`), and
  `compute()` swaps it. Immutability keeps the remapping function pure, so nothing
  can escape the lock.
- Integer-only fractional refill: store tokens scaled by 1e9 (nano-tokens).
  `capacity * 1e9` fits in a `long` for any capacity up to about 9.2e9.
- Refill is `refillTokens` per `refillPeriod` (Amendment 2). Tokens gained =
  `elapsed * refillTokens / periodNanos`; keep the remainder so no time is lost.
- **Overflow trap:** `elapsed * refillTokens` overflows after a long idle.
  Clamp the elapsed time to the time-to-full *before* multiplying. Clamping first
  also gives you the capacity cap.
- **Wrap trap:** compute the elapsed time as `now - last` (subtraction stays
  correct across a `Long.MAX_VALUE` wrap). Never compare `now > last`.
- Retry-After: the nanos until the next whole token, rounded up to whole seconds in
  integer math, clamped to at least 1. A combined call returns the longest wait. Test
  the value exactly at a boundary, 1 ns below it and 1 ns above it.
- Inject the clock as a `LongSupplier` (`System::nanoTime` in production).
- Keep the `BucketStore` constructor easy to extend. T05 adds a `maxTrackedKeys`
  parameter and a sweep method. The cap counts client addresses.
- Give `ClientBuckets` an "is full at time t" check built on the same refill math,
  so T05's sweep reuses it instead of copying it.

## Tests that already cover the area

None. This is greenfield.

## Suggested first edit (tests first)

`TokenBucketTest` with a mutable fake clock (`AtomicLong::get`). Write a full burst
and a deny first, then refill after a known elapsed time, then the rounding cases,
the long-idle cap and the wrap. `BucketStoreConcurrencyTest`: on a frozen clock, N
threads (more than C) wait on a `CountDownLatch` and then race `tryConsume` on one
key. Assert exactly C allows, and repeat 200+ times. Add a two-group case: with
`ORDERS` empty, a combined call must not take a token from `GENERAL`.

## Verify

`.\mvnw.cmd -q test -Dtest=TokenBucketTest,BucketStoreConcurrencyTest`, then the full
suite. `pom.xml` must be unchanged.
