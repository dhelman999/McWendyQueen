# Prime: shared contract for T01-T07

Primed 2026-10-21 on `demo/rate-limiting`. Owner decisions were folded in the same
day. Baseline at `bc299e0` was green: `.\mvnw.cmd -q test` ran 25 tests with 0
failures, `package` passed, and `GET /api/v1/menu` returned 200. Repo rules and the
verify loop are in `CLAUDE.md`. The code reference is `docs/CODEBASE.md`.

Crew agents work from these files, not from the chat that produced them. The types
that cross ticket boundaries are pinned here. Agents implement against these names,
and changing one means editing this file first.

## Owner decisions (2026-10-21)

1. **Test config is live.** `bc299e0` moved the test config to
   `src/test/resources/application.yml`. That file *replaces* the main
   `application.yml` for every test, because Boot loads the first
   `classpath:/application.yml` and test-classes comes first. Nothing in it merges
   with the main file. T03 works as written: its small fixed test values go under
   `app.rate-limit` in that file (values below).
2. **One store entry per client address.** The entry holds both route-group
   buckets. This amends the accepted spec, which keyed by (client, route group).
   It is recorded in T02.
3. **`RateLimitConfig` is a shared file.** T04 creates it and T05 edits it, so T05
   runs after T04 lands. See "Shared files".
4. **The access log is out of scope.** It is a separate draft ticket, T07, with the
   owner TBD. It writes client addresses to disk, and the PRD handles privacy
   separately. T07 does not block T01-T05.
5. **`RateLimitProperties` is registered exactly once**, as a `@Component`. See
   "Properties registration".

## Cross-ticket types

Format: name (owner -> users): shape.

- `RequestIdFilter.ORDER` (T01 -> T04): `public static final int`, value
  `Ordered.HIGHEST_PRECEDENCE + 10`.
- `ratelimit.RouteGroup` (T02 -> T04): enum `GENERAL`, `ORDERS`.
- `ratelimit.BucketLimits` (T02 -> T04): record
  `(long capacity, long refillTokens, Duration refillPeriod)` (T02, Amendment 2).
- `ratelimit.BucketStore` (T02; T04 calls it, T05 edits it):
  `RateLimitDecision tryConsume(String clientKey, RouteGroup... groups)`. Backed by
  `ConcurrentHashMap<String, ClientBuckets>` keyed by client address, with one
  bucket per `RouteGroup` inside the value (decision 2). Takes a `LongSupplier`
  nanosecond clock.
- `ratelimit.RateLimitDecision` (T02 -> T04): allowed, or denied with
  `long retryAfterSeconds` (>= 1).
- `ratelimit.RateLimitProperties` (T03 -> T04, T05): `app.rate-limit.*`, with
  `groups.general` / `groups.orders` as fixed fields. It is a `@Component`.
- `ratelimit.RateLimitConfig` (T04; T05 edits it after T04): a `@Configuration` that
  builds the clock, store, filter and registration. **Shared file.**

## Properties registration

Two crew agents must never both register the properties class. That gives two beans
of one type and a `NoUniqueBeanDefinitionException`. *Verified:*
`@EnableConfigurationProperties` names its bean `app.rate-limit-<FQCN>`, which
differs from the `@Component` bean name `rateLimitProperties`, so Spring does not
merge the two.

- `RateLimitProperties` is
  `@Component @Data @Validated @ConfigurationProperties("app.rate-limit")`, the same
  pattern as `kafka/KafkaAppConfig`. T03 owns it.
- `RateLimitConfig` does **not** carry `@EnableConfigurationProperties`. It
  receives `RateLimitProperties` as a `@Bean` method parameter.
- Only test contexts enable it, because they do not scan `@Component`s. A
  `@WebMvcTest` slice adds `@EnableConfigurationProperties(RateLimitProperties.class)`,
  and an `ApplicationContextRunner` gets a small user configuration that does the
  same. The slice already includes `ValidationAutoConfiguration` (*verified*), so
  validation runs there too.

## Test values (in `src/test/resources/application.yml`, owned by T03)

Every test context sees these values, so they stay small and fixed. T04 and T05 rely
on them. Changing one means editing this section and the tests that use it.

```yaml
app:
  rate-limit:
    enabled: true
    max-tracked-keys: 1000
    sweep-interval: 1m
    groups:
      general: { capacity: 3, refill-tokens: 1, refill-period: 1s }
      orders:  { capacity: 2, refill-tokens: 1, refill-period: 10s }
```

## Shared files and run order

| File | Created by | Also edited by | Rule |
|---|---|---|---|
| `ratelimit/RateLimitConfig.java` | T04 | T05 | T05 starts after T04 lands |
| `ratelimit/BucketStore.java` | T02 | T05 | T04 only calls it |
| `src/test/resources/application.yml` | (`bc299e0`) | T03 | edit only the `app.rate-limit` block |
| `src/main/resources/application.yml` | (existing) | T03, then T06 | edit only the `app.rate-limit` block |

- Wave 1 (T01, T02, T03): no shared files. They are safe to run in parallel without
  worktrees.
- Wave 2: T04, **then** T05. They run in sequence, not in parallel.
- Wave 3: T06 after T03 and a baseline. T07 is a draft outside the waves.

## Traps that cut across tickets

1. **The test `application.yml` replaces the main one** (decision 1). A property
   the main file sets, such as `server.port` or the Kafka config, does not exist in
   tests. If a test needs it, add it to the test file. Do not move test values into
   `@TestPropertySource` when the test yml is the right home. Keep
   `@TestPropertySource` for single-class overrides such as `enabled=false`.
2. **`@WebMvcTest` slices include every `Filter` and `FilterRegistrationBean`**
   (verified in `WebMvcTypeExcludeFilter`, Boot 3.5.0) but no `@Component`
   services. A `@Component` `RateLimitFilter` would land in all 5 existing controller
   test slices without `RateLimitProperties`, and those tests would break. Build the
   filter inside `RateLimitConfig`.
3. **MockMvc honors registration URL patterns and order** (verified in
   `SpringBootMockMvcBuilderCustomizer`). Filter-order and `/api/v1/*` scoping
   tests are real tests, not mocks.
4. Filter order is resolved by `AnnotationAwareOrderComparator`, so `@Order` or
   `Ordered` on a `@Component` filter both count (verified). An unordered filter
   sorts as `LOWEST_PRECEDENCE`, which is where `RequestIdFilter` sat before T01.
5. Boot's own filters: `CharacterEncodingFilter` runs at `HIGHEST_PRECEDENCE`,
   `FormContentFilter` at -9900, `RequestContextFilter` at -105. `HIGHEST + 10`
   keeps request ids after character encoding and ahead of everything else.

## Tests already written (2026-10-21)

T01-T04 have failing tests against compile-only stubs (`STUB(Tnn)` comments in main
code). Crew agents implement until those tests are green and do not edit their
assertions. **T05 deliberately has none**: its crewmate writes its own tests, so
the run can be compared against the pre-written tickets. Do not add T05 tests from
these briefs. Details are in each ticket's Validation section.

The test yml also carries Kafka-off and springdoc settings, so `@SpringBootTest`
contexts start without a broker (`RateLimitAcceptanceTest`).

## Per-ticket briefs

`prime-T01.md` through `prime-T06.md`. Each brief gives the current behavior,
traps, existing test coverage and a first edit, and assumes the agent has read this
file. T07 is a draft ticket with no brief yet.
