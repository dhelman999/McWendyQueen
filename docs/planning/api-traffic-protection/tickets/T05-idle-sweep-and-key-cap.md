# T05: Idle-bucket sweep and tracked-key cap

- Slug: `api-traffic-protection`
- Size: S
- Depends on: T02, T03, T04 (shared file `RateLimitConfig`, so this ticket runs
  after T04 lands)
- PRD: `docs/planning/api-traffic-protection/prd.md`
- Spec: `docs/planning/api-traffic-protection/spec.md`

## Goal

Memory stays bounded during the public promotion. Buckets idle long enough to have
refilled completely are removed on a schedule, which loses no state because such a
bucket is the same as a new one. A hard cap on tracked keys backs up the sweep if
many addresses arrive at once.

## Acceptance criteria

- [ ] A sweep runs every `sweep-interval` and removes only buckets that are full at
      sweep time. Removal is atomic per key, so it never races a concurrent
      `tryConsume` on the same key.
- [ ] Scheduling is local to the rate limiter. The app has no `@EnableScheduling`
      today; turning it on app-wide is a decision recorded here, not a side effect.
      **Decision (2026-10-22):** local. `BucketSweeper` owns one named daemon
      executor thread, and Spring stops it through the inferred `close()`. No
      app-wide `@EnableScheduling`: it would switch on any future `@Scheduled`
      method for one internal timer. Revisit if a second periodic job appears.
- [ ] When `max-tracked-keys` is reached, a new key passes through unlimited and a
      WARN is logged at most once per sweep interval (the spec's recommended
      default: an address spray is DDoS, a non-goal).
- [ ] A swept key seen again behaves like a fresh, full bucket.

## Definition of done

- Acceptance criteria all checked.
- Tests with the injectable clock cover: full buckets swept, part-drained buckets
  kept, the cap reached (pass-through plus a single WARN), and a sweep running
  while `tryConsume` runs on another thread.
- The sweep stops cleanly on application shutdown (no leaked thread in tests).
- Change reviewed through skill `triple-check`.

## Likely files

- `src/main/java/com/mcwendyqueen/ratelimit/BucketStore.java`
- `src/main/java/com/mcwendyqueen/ratelimit/RateLimitConfig.java` (**shared file**,
  created by T04: wire the sweeper and pass `max-tracked-keys` into the store)
- `src/main/java/com/mcwendyqueen/ratelimit/BucketSweeper.java` (new)
- `src/test/java/com/mcwendyqueen/ratelimit/BucketSweeperTest.java` (new)

## Validation (do this before calling the ticket done)

- Unit: `./mvnw test -Dtest=BucketSweeperTest`
- Full suite: `./mvnw test`
- Manual: none beyond the tests; the cap path cannot be reached by hand without an
  address spray.

Write failing tests first when implementing this ticket (skill `validation-tdd`). Ship still goes through skill `triple-check`.
