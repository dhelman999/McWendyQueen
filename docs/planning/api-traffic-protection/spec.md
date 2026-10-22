# Spec: API traffic protection (per-client rate limiting, v1)

- Slug: `api-traffic-protection`
- PRD: `docs/planning/api-traffic-protection/prd.md` (accepted 2026-10-19)
- Research: `docs/planning/api-traffic-protection/research.md`
- Status: accepted (2026-10-19, owner review)
- Date: 2026-10-19
- Amended: 2026-10-21. The bucket store is keyed by client address alone, with one
  bucket per route group (see `tickets/T02-token-bucket-store.md`, Amendment).
- Amended: 2026-10-21. Refill is tokens per period, not tokens per second (see T02,
  Amendment 2).

## Problem and goals

Any single client can send unlimited requests to the `/api/v1` API, and a runaway
script or an abusive client can slow it down for everyone. Order placement is
already one of the slowest calls. Part of the API is about to open to the public
for a promotion. v1 throttles the noisy client with "too many requests" while
everyone else stays fast. It adds no new infrastructure and no new libraries, and
ops change limits by editing config. Every decision below is judged against this.

## Approaches considered

| Approach | Trade-offs | Notes |
|----------|------------|-------|
| **A. In-app filter with our own in-memory token bucket** | No new dependencies. We own about 150 lines of concurrency and refill code, so the tests must cover it. | Meets every PRD constraint. **Chosen.** |
| B. Same filter, using Bucket4j and Caffeine | Well-tested code and the least bug risk, but it adds 2 libraries. | Breaks "no new libraries". Kept as the v2 path. |
| C. Rate limiting at the load balancer or a proxy | No app code, but it needs new infrastructure, and nobody knows today what sits in front of the service. | Out of scope for v1. |

Chosen: **A**, because it meets the PRD's constraints and is reversible. The filter
and its config don't depend on how the bucket works inside, so B can replace the
inside later without changing them.

## Recommended approach

A servlet filter that runs before Spring MVC for every request under `/api/v1/*`:

1. **Identify the client** by `request.getRemoteAddr()`. API keys come later.
2. **Skip exempt routes:** `/api/v1/admin/**` passes through unlimited, as the PRD
   says.
3. **Classify the request** into a route group:
   - `orders`: `POST /api/v1/orders`, which gets its own tighter limit.
   - `general`: everything else under `/api/v1`.
4. **Take a token** from the client's bucket for its route group (one store entry
   per client address, holding a bucket per group; amended, see T02). A request in
   the `orders` group also takes one from the client's `general` bucket (settled,
   see T04 and the open questions).
5. **Allowed:** pass the request on unchanged. **Over the limit:** stop the request
   and answer **429 Too Many Requests** with `Retry-After: <seconds>`, rounded up,
   and a small JSON error body. Write one WARN log line with the client address,
   the route group and the request id.

The API docs (`/openapi`, `/swagger-ui.html`, `/swagger-ui/**`) are outside
`/api/v1`, so the filter never sees them. The PRD's exemption for health checks has
nothing to apply to yet: the app has no health endpoint.

### Where it plugs in

- A new `ratelimit` package beside the existing `exception` package. It holds the
  filter, the bucket store and the properties class.
- The filter is registered through a `FilterRegistrationBean` on `/api/v1/*`, with
  an explicit order **after** `RequestIdFilter`. That way the request id is already
  in the logging context (MDC) when the filter logs a throttle.
  `RequestIdFilter` (`exception/RequestIdFilter.java`) is ordered at
  `RequestIdFilter.ORDER` (T01).
- The filter writes its own 429 response. `GlobalExceptionHandler` never sees errors
  thrown in a filter. The 429 body should match the shape of the app's other error
  responses.
- Route groups and the admin exemption reuse the paths in `ApiConstants`.

## Key decisions

- **Stack and libraries:** the JDK and Spring Boot 3.5.0 only. The per-client state
  is a `ConcurrentHashMap`, and each key is updated atomically with `compute()`.
  Time comes from `System.nanoTime()` with integer arithmetic, never the wall clock.
  Bucket4j, Resilience4j and Guava were considered (see `research.md`); Bucket4j is
  the v2 path.
- **Algorithm:** a token bucket with two numbers per bucket, capacity (the burst)
  and refill rate. It allows the burst of calls a normal page load makes and
  throttles sustained traffic. It doesn't let a client double its limit at a window
  boundary, and the retry time is one formula.
- **Data model:** in memory only, nothing persisted. Key = (client address, route
  group); value = (tokens, last refill time). *Amended 2026-10-21:* key = client
  address, value = one (tokens, last refill time) per route group (see T02). A
  scheduled sweep removes buckets idle long enough to have refilled completely. Such a
  bucket is identical to a new one, so no state is lost. A hard cap on tracked keys
  backs up the sweep. All state resets on restart, and that is acceptable.
- **Configuration:** `app.rate-limit.*`, bound through `@ConfigurationProperties`
  with validation. Ops edit `application.yml` or environment variables, and the
  change takes effect on restart. Shape:

  ```yaml
  app:
    rate-limit:
      enabled: true
      max-tracked-keys: 100000   # the demo ships 10000 (owner, 2026-10-22)
      sweep-interval: 1m
      groups:
        general: { capacity: TBD, refill-tokens: TBD, refill-period: TBD }
        orders:  { capacity: TBD, refill-tokens: TBD, refill-period: TBD }
  ```

  The limit values wait on the ops traffic baseline. The defaults must be generous
  so that normal use never sees a 429. `enabled: false` turns the limiter off
  entirely.
- **Boundaries:**
  - Volume protection only: no authentication and no secrets.
  - Forwarded headers (`X-Forwarded-For`, `Forwarded`) are ignored, and
    `server.forward-headers-strategy` stays at its default. Reading them by hand
    would let any client spoof a fresh address on every request.
  - Limits are per instance, which matches today's single instance.
- **Observability:** logs only for v1. Actuator and Micrometer are not added.

## Missing pieces

- An explicit order on `RequestIdFilter` (done in T01).
- Limit values, which need the ops traffic baseline (about a week of monitoring).
- A 429 error body in the app's error-response shape.
- Tests:
  - Bucket arithmetic: refill and `Retry-After` rounding, using an injectable clock.
  - Concurrency: many threads on one key must not overspend the bucket.
  - Filter tests: route groups, the admin exemption, the 429 response, the docs paths staying outside
    the filter, `enabled: false`, and the log line carrying the request id.

## Spikes

None for v1. Every decision is reversible behind the filter. The one real unknown,
what sits in front of the service, is out of scope by the PRD.

## v2 (TBD — needs validation)

- Bucket4j for the bucket, and Redis for limits shared across several instances.
  That is new infrastructure, so it needs its own PRD.
- API keys as the client identity, replacing the source address.
- Limits that change while the service keeps running.
- Reading the client address from a trusted load balancer's forwarded header, once
  the production network path is known.

## Open questions

Left open on purpose (owner, 2026-10-19). The implementing developer settles them,
using the recommendations as the default.

- [x] Does an order also count against the client's `general` bucket? Recommended:
      yes, so one client's total traffic stays bounded. **Settled: yes** (T04).
- [x] When the tracked-key cap is hit: let requests through and log it
      (recommended, because an address spray is DDoS, a non-goal), or answer 429?
      **Settled: let through, one WARN per sweep interval** (T05).
- [ ] Shared addresses (carrier NAT, office Wi-Fi): accept as a known v1 limitation
      and mitigate with generous limits and throttle logs? (Recommended; the owner
      has not chosen.)
- [ ] Limit values for `general` and `orders`: need the ops baseline.
- [ ] **Throttle log names the request's group, not the bucket that denied it**
      (raised 2026-10-22 in `triple-check`). After a client drains `general`, a
      `POST /orders` is refused by `general` (`Retry-After` 1) but the WARN line says
      `group=orders`, which points ops at the wrong limit. Fixing it means adding the
      denying group to `RateLimitDecision`, a type T02 owns and T04 consumes.
      Accepted for the demo; decide before ops tune limits from these logs (T06).
- [ ] Add `RateLimit` / `RateLimit-Policy` headers? They are still an IETF draft.
      Recommended: not in v1; `Retry-After` is the standard hint.
