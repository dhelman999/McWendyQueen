# T04: Rate-limit filter with 429 response and throttle log

- Slug: `api-traffic-protection`
- Size: M
- Depends on: T01, T02, T03
- PRD: `docs/planning/api-traffic-protection/prd.md`
- Spec: `docs/planning/api-traffic-protection/spec.md`

## Goal

The end-to-end behavior from the PRD. A single client that goes over its limit on
`/api/v1` gets **429 Too Many Requests** with `Retry-After`, while other clients and
normal traffic are unaffected. `POST /api/v1/orders` has its own, tighter limit.
Every throttle writes one WARN line ops can trace.

## Acceptance criteria

- [ ] A servlet filter registered with a `FilterRegistrationBean` on `/api/v1/*`,
      ordered after `RequestIdFilter` (T01's constant).
- [ ] The client is identified by `request.getRemoteAddr()` only. `X-Forwarded-For`
      and `Forwarded` are ignored, and `server.forward-headers-strategy` stays at
      its default.
- [ ] `/api/v1/admin/**` passes through unlimited. Paths are built from
      `ApiConstants`, not string literals.
- [ ] `POST /api/v1/orders` is the `orders` group; everything else under `/api/v1`,
      including `GET /api/v1/orders`, is `general`.
- [ ] The spec's open question is settled: an `orders` request also counts against
      `general` (the recommended default), unless the implementer records a
      different decision in this ticket.
- [ ] Over the limit: status 429, `Retry-After` in whole seconds, and a JSON body in
      the same shape as `BaseExceptionMessage.response()` (`status`, `code`, `type`,
      `message`, `timeStamp`, `requestId`). The controller is not called.
- [ ] One WARN line per throttled request with the client address, route group and
      request id.
- [ ] With `enabled: false` the filter passes everything through.

## Definition of done

- Acceptance criteria all checked.
- `MockMvc` tests, with the remote address set per request, cover: under the limit
  passes; over the limit gets 429 with `Retry-After` and the body shape; a second
  client is unaffected while the first is throttled; the `orders` / `general`
  split; the admin exemption; a forwarded header that does not change identity;
  `enabled: false`; and the log line carrying the request id (output capture).
- `/openapi` and `/swagger-ui.html` still load and are never limited.
- The E2E check below has been run against the running app and its output is noted
  in the PR.
- Change reviewed through skill `triple-check`.

## Likely files

- `src/main/java/com/mcwendyqueen/ratelimit/RateLimitFilter.java` (new)
- `src/main/java/com/mcwendyqueen/ratelimit/RateLimitConfig.java` (new, registration;
  **shared file**, T05 edits it after this ticket lands)
- `src/main/java/com/mcwendyqueen/exception/` (a 429 message type, if that keeps
  the body shape in one place)
- `src/test/java/com/mcwendyqueen/ratelimit/RateLimitFilterTest.java` (new)

## Validation (do this before calling the ticket done)

Failing tests are already written against stubs (2026-10-21). Implement until they
are green; do not edit their assertions.

- `RateLimitFilterTest` (15): 13 red on assertions.
- `RateLimitAcceptanceTest` (5, real Tomcat on 127.0.0.x): all red. It covers
  path parameters. `/orders;x=1` and `/v1;v=1/orders` both route to `createOrder`
  on Tomcat (verified 2026-10-21), so classify on the routed path.
- `RateLimitDisabledTest` (1): green by design.

Two `RateLimitFilterTest` tests are green on the stub by design:
`admin_isExemptAndDoesNotSpendGeneral` and `allowedRequest_logsNoThrottleLine`.
Each is a no-regression guard and only means something beside its red
neighbours. The frozen clock is a `@Primary LongSupplier`, so `RateLimitConfig`
must take the clock as a `LongSupplier` bean.

- Acceptance: `./mvnw test -Dtest=RateLimitAcceptanceTest` (no Docker needed)

- Slice: `./mvnw test -Dtest=RateLimitFilterTest`
- Full suite: `./mvnw test`
- E2E (manual): start the app with a small `orders` capacity, then call
  `POST localhost:3000/api/v1/orders` in a loop with curl, printing the status
  code each time. Expect non-429 codes up to the capacity, then 429; `curl -i` on a
  throttled call shows `Retry-After`. An admin endpoint and `/swagger-ui.html`
  still answer during the throttle.

Write failing tests first when implementing this ticket (skill `validation-tdd`). Ship still goes through skill `triple-check`.
