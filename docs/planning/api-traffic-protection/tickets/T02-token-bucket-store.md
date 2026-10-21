# T02: Token bucket and bucket store

- Slug: `api-traffic-protection`
- Size: M
- Depends on: none
- PRD: `docs/planning/api-traffic-protection/prd.md`
- Spec: `docs/planning/api-traffic-protection/spec.md`

## Goal

A pure-Java token bucket and a thread-safe store of buckets keyed by (client
address, route group), with no Spring and no servlet code. It decides allow or deny
and computes the retry time. This is the roughly 150 lines of concurrency and refill
code the spec says we own and must test well. It sits behind an interface so
Bucket4j (the v2 path) could replace the inside without the filter changing.

## Acceptance criteria

- [ ] New package `com.mcwendyqueen.ratelimit`.
- [ ] A bucket has two parameters: capacity (the burst) and refill rate in tokens
      per second. It starts full.
- [ ] Time comes from an injectable nanosecond clock (`System.nanoTime()` in
      production). Integer arithmetic only: no wall clock, no `double`.
- [ ] `tryConsume(key)` returns either allowed, or denied with a retry time in
      whole seconds, rounded up and at least 1.
- [ ] The store is a `ConcurrentHashMap`, and each key is updated atomically with
      `compute()`. No bucket state is read or written outside it.
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
- `src/test/java/com/mcwendyqueen/ratelimit/TokenBucketTest.java` (new)
- `src/test/java/com/mcwendyqueen/ratelimit/BucketStoreConcurrencyTest.java` (new)

## Validation (do this before calling the ticket done)

- Unit: `./mvnw test -Dtest=TokenBucketTest,BucketStoreConcurrencyTest`
- Full suite: `./mvnw test`
- Manual: none. This ticket has no HTTP surface.

Write failing tests first when implementing this ticket (skill `validation-tdd`). Ship still goes through skill `triple-check`.
