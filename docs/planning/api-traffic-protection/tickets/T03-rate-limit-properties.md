# T03: Rate-limit configuration properties

- Slug: `api-traffic-protection`
- Size: S
- Depends on: none
- PRD: `docs/planning/api-traffic-protection/prd.md`
- Spec: `docs/planning/api-traffic-protection/spec.md`

## Goal

Ops set every limit in `application.yml` or with environment variables, and a
change takes effect on restart with no code change. Bad values stop the app at
startup with a clear message instead of failing quietly at request time.

## Acceptance criteria

- [ ] A `@ConfigurationProperties("app.rate-limit")` class with `@Validated`,
      matching the spec's shape: `enabled`, `max-tracked-keys`, `sweep-interval`,
      and `groups.general` / `groups.orders`, each with `capacity` and
      `refill-per-second`.
- [ ] Validation: capacity, refill and `max-tracked-keys` are at least 1, and
      `sweep-interval` is positive. An invalid value fails startup and the message
      names the property.
- [ ] `application.yml` gets an `app.rate-limit` block with deliberately generous
      placeholder values. A comment says the real values wait on the ops baseline
      (T06).
- [ ] Every value can be overridden by an environment variable through Spring's
      relaxed binding (for example `APP_RATELIMIT_GROUPS_ORDERS_CAPACITY`); a test
      proves one.
- [ ] `enabled: false` is exposed for T04 to switch the limiter off entirely.
- [ ] The test `application.yml` sets small, fixed values the T04 tests can rely on.

## Definition of done

- Acceptance criteria all checked.
- Binding tests (`ApplicationContextRunner` or a small `@SpringBootTest`) cover the
  defaults, an override, and an invalid value failing startup.
- No new dependency (`spring-boot-starter-validation` is already in the POM).
- Change reviewed through skill `triple-check`.

## Likely files

- `src/main/java/com/mcwendyqueen/ratelimit/RateLimitProperties.java` (new)
- `src/main/resources/application.yml`
- `src/test/java/resources/application.yml`
- `src/test/java/com/mcwendyqueen/ratelimit/RateLimitPropertiesTest.java` (new)

## Validation (do this before calling the ticket done)

- Unit: `./mvnw test -Dtest=RateLimitPropertiesTest`
- Full suite: `./mvnw test`
- Manual: start the app with `APP_RATELIMIT_GROUPS_ORDERS_CAPACITY=0` and confirm
  startup fails with a message naming the property.

Write failing tests first when implementing this ticket (skill `validation-tdd`). Ship still goes through skill `triple-check`.
