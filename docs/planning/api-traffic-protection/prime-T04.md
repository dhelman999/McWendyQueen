# Prime: T04 rate-limit filter, 429 and throttle log

Read `prime-shared.md` first. This ticket depends on the T01-T03 contract names.
`RateLimitConfig` is a shared file: T04 creates it, and T05 edits it after T04
lands.

## Current behavior and landing points

- Paths: `OrdersController` maps `@PostMapping(ORDER_PATH)` under
  `API_BASE_PATH + V1_PATH`. Admin is `API_BASE_PATH + V1_PATH + ADMIN_PATH`
  (`KafkaAdminListenerController`). Build the URL pattern and the classification
  from those constants.
- Error shape: `ExceptionFactory.createExceptionMessage(new
  ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS))` already returns a
  `SpringHttpErrorMessage` with `code` 429, `status` "Too Many Requests",
  `requestId` from the MDC, and a timestamp. `response(true)` is the AC's shape
  without `errors`. That covers the 429 body with no new message class. Add a
  subclass only if you want a specific message text.
- Serialize with the Spring `ObjectMapper` bean, injected into `RateLimitConfig`.

## `RateLimitConfig` (T04 creates it)

- A `@Configuration` with no `@EnableConfigurationProperties` (see "Properties
  registration"). Its `@Bean` methods take `RateLimitProperties` as a parameter.
- Beans:
  - a `LongSupplier` clock named `rateLimitClock`, set to `System::nanoTime`;
  - the `BucketStore`;
  - the `RateLimitFilter`;
  - a `FilterRegistrationBean<RateLimitFilter>` on `API_BASE_PATH + V1_PATH + "/*"`
    with order `RequestIdFilter.ORDER + 10`.
- Leave room for T05. It adds a sweeper bean and passes `max-tracked-keys` into the
  store, so keep the store's construction in one `@Bean` method that T05 can extend.

## Traps

1. **Do not make the filter a `@Component`** (shared trap 2). *Assumption
   (unverified):* `@WebMvcTest` leaves out scanned `@Configuration` classes, so the
   new tests need `@Import(RateLimitConfig.class)` and existing tests are
   unaffected. Check this with the first test you write.
2. **Use the shared test values; do not invent new ones.** `general` is 3, 1 token per
   1s, and `orders` is 2, 1 token per 10s, from the test yml. Since an order also
   counts against `general`, two `POST`s drain `orders`, and a third gets 429 from
   `orders`. `general` then has 1 token left, so one `GET` passes and the next gets
   429.
3. **Freeze the clock in tests.** At 1 token per 1s, a slow run can refill a token
   mid-test and make it flaky. Recommended: a `@TestConfiguration` provides a
   `@Primary` `LongSupplier` backed by an `AtomicLong`. Bean overriding is off by
   default in Boot 3.5, so use `@Primary` and not the same bean name. With a frozen
   clock, `Retry-After` is 1 for `general` and 10 for `orders`. The tests already
   exist and expect exactly that.
4. **Bucket state persists across test methods**, because the Spring context is
   cached. MockMvc's default remote address is 127.0.0.1, so every test would be one
   client. Give each test its own address with
   `.with(r -> { r.setRemoteAddr("10.0.0.N"); return r; })`.
   `enabled: false` needs its own context: use a separate class, or a `@Nested`
   class with `@TestPropertySource(properties = "app.rate-limit.enabled=false")`.
5. **Classify on the path Tomcat routes on, not the raw `getRequestURI()`.** Probed
   on the running app (2026-10-21): dot segments such as `/api/v1/admin/../orders`
   return 404, so they are no bypass. Path parameters do route, though:
   `POST /api/v1/orders;x=1` and `POST /api/v1;v=1/orders` both reach
   `createOrder`, and `/api/v1/admin;x/kafka/listeners` reaches admin. A raw-URI
   match would put those orders in `general`. `RateLimitAcceptanceTest` covers the
   orders cases on real Tomcat, which MockMvc cannot show.
6. The console pattern does not print the MDC, so put `requestId` in the WARN
   message text itself. Capture it in the test with `OutputCaptureExtension`.
7. `POST /orders` is slow and random: 50% approval, `Thread.sleep` retries, and a
   500 or 503 on denial (known issue 1). For the manual run, set `orders` to
   capacity 3, 1 token per 10s. `RateLimitAcceptanceTest` already mocks
   eligibility. Expect mixed non-429 codes, then 429. It writes rows to
   local Postgres, so clean up afterwards with `DELETE /api/v1/orders/all`.

## Tests that already cover the area

`OrdersControllerTest` and the other 4 `@WebMvcTest` classes must stay green, which
confirms trap 1. They do not exercise rate limiting.

## Suggested first edit (tests first)

Write `RateLimitFilterTest` with:
- `@WebMvcTest(controllers = {MenuItemsController.class, OrdersController.class,
  KafkaAdminListenerController.class})`;
- `@Import({RateLimitConfig.class, FrozenClockConfig.class})`;
- `@EnableConfigurationProperties(RateLimitProperties.class)`;
- `@MockBean` for each controller's service.

Start with "over the limit returns 429 with Retry-After and the body shape". Then add
the second client, the orders/general split from trap 2, the admin exemption, an
ignored `X-Forwarded-For`, and the log line.
