# Prime: T03 rate-limit configuration properties

Read `prime-shared.md` first, especially "Properties registration" and "Test
values".

## Current behavior

- The only `@ConfigurationProperties` precedent is `kafka/KafkaAppConfig`:
  `@Component @Data @ConfigurationProperties("kafka-topics.prod")`, with no
  validation. `RateLimitProperties` follows the same pattern and adds `@Validated`.
  This ticket is its only registration. Do not add `@EnableConfigurationProperties`
  in main code.
- `src/main/resources/application.yml` has an `app:` block that holds only
  `debug-features-enabled`. Add `rate-limit:` under it, with generous placeholders.
- `src/test/resources/application.yml` (added in `bc299e0`) replaces the main file
  for tests. Add the `app.rate-limit` block from "Test values" in
  `prime-shared.md`, exactly as written, since T04 and T05 rely on those numbers.
- `spring-boot-starter-validation` brings Hibernate Validator 8.0.2. That version
  has `@DurationMin` (verified in the jar), so `sweep-interval` can use
  `@NotNull @DurationMin(nanos = 0, inclusive = false)`.

## Extension points and traps

- **Nested validation needs `@Valid`** on the `groups` field and on each group
  field, or the `@Min(1)` constraints inside never run. Make `general` and `orders`
  `@NotNull` fixed fields, not a `Map`, so a missing group fails startup.
- `max-tracked-keys`: `@Min(1)`, with a type of `int` or `long`. T05 compares it
  with `ConcurrentHashMap.size()`, which returns `int`.
- On a bind failure, Boot's failure analyzer prints
  `Property: app.rate-limit.groups.orders.capacity`. That message satisfies "names
  the property".
- **`ApplicationContextRunner` reads no `application.yml` at all.** Because the test
  file shadows the main one on the classpath, `classpath:application.yml` in a
  test is the *test* file. Recommended split:
  - Test values: load `classpath:application.yml` with `YamlPropertySourceLoader`
    into the runner, and assert the "Test values" numbers.
  - Production placeholders: load `src/main/resources/application.yml` as a file
    resource (Maven runs tests from the repo root), and assert that it binds and
    validates. This guards the ops-facing file that T06 later edits.
- Environment override: add a `SystemEnvironmentPropertySource` containing
  `APP_RATELIMIT_GROUPS_ORDERS_CAPACITY` in a runner initializer. Relaxed binding
  drops the dash in `rate-limit`.
- Placeholder values must be generous, for example `general` at 1000 / 100 and
  `orders` at 100 / 10, with a comment that points to T06.

## Tests that already cover the area

None.

## Suggested first edit (tests first)

Write `RateLimitPropertiesTest` with an `ApplicationContextRunner`. Its user
configuration carries `@EnableConfigurationProperties(RateLimitProperties.class)`,
and it adds `AutoConfigurations.of(ValidationAutoConfiguration.class)`. Cover five
cases:
- the test values bind;
- the main-file placeholders bind;
- an environment override wins;
- `capacity: 0` fails with the property path in the root cause;
- a zero `sweep-interval` fails.

## Verify

`.\mvnw.cmd -q test -Dtest=RateLimitPropertiesTest`, then the full suite. For the
manual check, start the app with `APP_RATELIMIT_GROUPS_ORDERS_CAPACITY=0` and expect
the property to be named in the startup failure. Postgres and Kafka must be up, or
an unrelated failure can hide it.
