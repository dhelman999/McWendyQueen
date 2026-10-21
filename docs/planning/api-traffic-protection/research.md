# Research: rate-limiting options (input for the spec)

- Date: 2026-10-19
- Method: three read-only research agents (algorithms, Java libraries, Spring for
  Boot 3.5.0). No repo files were changed.
- Status: input only. The spec makes the decisions.

## Consensus

A `OncePerRequestFilter` inside the app, registered with a `FilterRegistrationBean`
on `/api/v1/*`. It keeps a **token bucket per source address** in memory, answers
**429 with `Retry-After`** in seconds, and reads its limits from
`@ConfigurationProperties` (a restart applies changes). The JDK and Spring already
provide all of this, so the PRD's "no new libraries" rule holds. If libraries are
allowed later: Bucket4j (`bucket4j_jdk17-core` 8.21.0) with a Caffeine 3.x cache.

## Algorithms

| Algorithm | Bursts | Memory / client | Window edges | Retry hint | Fit |
|---|---|---|---|---|---|
| Fixed window | Up to 2x the limit at a window boundary | 1 counter | Poor | Trivial | No |
| Sliding window log | None | 1 timestamp per request | Exact | Easy | No (memory) |
| Sliding window counter | Smoothed | 2 counters | Close | Approximate | Maybe |
| **Token bucket** | Burst of B, then rate r | 2 numbers | No edges | `ceil((1 - tokens) / r)` | **Recommended** |
| Leaky bucket (queue) | Queued, adds delay | A queue | Exact | Awkward | No |
| GCRA | Same as a token bucket (same maths) | 1 long | No edges | `TAT - tolerance - now` | Alternative build |

- Allowing bursts keeps normal page loads unaffected. Ops tune two numbers: burst
  size and refill rate. Orders get a second bucket keyed by address and route group.
- Use `System.nanoTime` and integer arithmetic; avoid wall-clock time and float drift.
- Bound the map: a scheduled sweep drops entries idle long enough to be full again
  (this loses no state), backed by a hard cap on entries. **Spec decision:** behaviour
  when the cap is hit. Recommended: fail open and log, because an address spray is
  DDoS, which is a non-goal.
- Headers: 429 comes from RFC 6585 §4 and `Retry-After` from RFC 9110 §10.2.3. The
  IETF `RateLimit` / `RateLimit-Policy` fields are still a draft
  (draft-ietf-httpapi-ratelimit-headers), so they are optional for v1.

## Java libraries (versions checked on Maven Central, 2026-10-19)

None of them is on the classpath today (119 jars checked from the last test run).
Boot 3.5.0 manages a Caffeine version (3.2.0), but nothing pulls Caffeine in.

| Option | Latest | Per client | Memory bounded | Notes |
|---|---|---|---|---|
| Hand-rolled `ConcurrentHashMap` + token bucket | n/a | Yes | We write it | ~100-150 lines plus concurrency tests. v1 choice. |
| Bucket4j `bucket4j_jdk17-core` | 8.21.0 | Through a map | Through Caffeine | Apache-2.0, no transitive dependencies, returns the wait time. The old `bucket4j-core` stopped at 8.10.1. |
| Caffeine | 3.3.1 | Store only | Size cap and idle expiry | Pairs with Bucket4j. |
| Resilience4j RateLimiter | 2.4.0 | One limiter per key | No (registry grows) | Built to protect one downstream service. Avoid. |
| Guava RateLimiter | 33.7.2-jre | No | No | `@Beta`, built around blocking, heavy jar. Avoid. |

## Spring (Boot 3.5.0 / Framework 6.2)

- Spring has no built-in per-client rate limiter. `@ConcurrencyLimit` arrives in
  Framework 7 / Boot 4 and caps concurrent calls, not request rate.
- Hook: a filter on `/api/v1/*` ordered after `RequestIdFilter`
  (`src/main/java/com/mcwendyqueen/exception/RequestIdFilter.java`, which has no
  explicit order today), so log lines carry the request id. A `HandlerInterceptor`
  is the second choice.
- The filter must write its own 429. `GlobalExceptionHandler` does not see errors
  thrown in filters.
- Exemptions from the PRD: API docs are already outside `/api/v1`; the filter must
  skip `/api/v1/admin/**` and any health check.
- Client address: `request.getRemoteAddr()`, with `server.forward-headers-strategy`
  left at its default. Never parse `X-Forwarded-For` by hand, because a client can
  spoof a fresh value on every request. Revisit if a load balancer is confirmed.
- Ruled out: Spring Cloud Gateway `RequestRateLimiter` (needs Redis and a separate
  proxy); `bucket4j-spring-boot-starter` 0.13.0 (the Boot 3.5 line, but a new library);
  `resilience4j-spring-boot3` 2.4.0 (a poor fit for per-key limits).
- Live limit changes (a follow-up) would need Spring Cloud Context plus Actuator, or
  a secured admin endpoint. There is no Spring Security today.
- **Spec decision:** Actuator/Micrometer would give 429 counts and a health endpoint.
  It is first-party but still a new dependency. v1 = logs only, per the PRD.

## Caveats

- The algorithms agent had no web access, so its sources come from its own
  knowledge. Check the IETF draft's current revision.
- Three Spring doc claims were not fetched (servlet filters, interceptors,
  `ForwardedHeaderFilter` guidance).
- Side note: the repo is on Boot 3.5.0; the latest 3.5.x is 3.5.16.

## Sources

- RFC 6585 §4: https://www.rfc-editor.org/rfc/rfc6585#section-4
- RFC 9110 §10.2.3: https://www.rfc-editor.org/rfc/rfc9110#section-10.2.3
- IETF RateLimit headers draft: https://datatracker.ietf.org/doc/draft-ietf-httpapi-ratelimit-headers/
- Cloudflare, sliding window counter: https://blog.cloudflare.com/counting-things-a-lot-of-different-things/
- Stripe, rate limiters: https://stripe.com/blog/rate-limiters
- Brandur Leach, GCRA: https://brandur.org/rate-limiting
- Spring Framework 7 resilience: https://docs.spring.io/spring-framework/reference/core/resilience.html
- Boot 3.5 forward headers: https://docs.spring.io/spring-boot/3.5/how-to/webserver.html
- Spring Cloud Gateway RequestRateLimiter: https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/gatewayfilter-factories/requestratelimiter-factory.html
- bucket4j-spring-boot-starter: https://github.com/MarcGiffing/bucket4j-spring-boot-starter
- Maven Central metadata for Bucket4j, Caffeine, Resilience4j, Guava and Spring Boot
