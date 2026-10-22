# T01: Give RequestIdFilter an explicit order

- Slug: `api-traffic-protection`
- Size: S
- Depends on: none
- PRD: `docs/planning/api-traffic-protection/prd.md`
- Spec: `docs/planning/api-traffic-protection/spec.md`

## Goal

`RequestIdFilter` runs at a known, explicit position in the filter chain, early
enough that every later filter (the rate limiter in T04) sees `requestId` in the
MDC. Today it is a bare `@Component` with no order, so its position is undefined.
Behavior for existing requests does not change.

## Acceptance criteria

- [ ] `RequestIdFilter` has an explicit order, exposed as a public constant so T04
      can order itself after it (for example `RequestIdFilter.ORDER`).
- [ ] The order puts it ahead of any filter the app adds later; a comment says why.
- [ ] Existing behavior is unchanged: a fresh UUID per request in MDC key
      `requestId`, and the MDC is cleared in `finally`.
- [ ] A test proves that a filter registered after it sees a non-null `requestId`
      in the MDC.

## Definition of done

- Acceptance criteria all checked.
- `./mvnw test` passes with no existing test changed.
- Change reviewed through skill `triple-check`.

## Likely files

- `src/main/java/com/mcwendyqueen/exception/RequestIdFilter.java`
- `src/test/java/com/mcwendyqueen/exception/RequestIdFilterTest.java` (new)

## Validation (do this before calling the ticket done)

Failing tests are already written (2026-10-21): `RequestIdFilterTest` (4), 3 red on
assertions. The stub has `ORDER`, but the filter is not ordered yet.
`requestIdFilter_afterRequest_clearsMdc` is a characterization test, green before
and after. Implement until all 4 are green; do not edit their assertions.

- Unit: `./mvnw test -Dtest=RequestIdFilterTest`
- Full suite: `./mvnw test`
- Manual: run the app, call an `/api/v1` endpoint that errors, and confirm the
  error body's `requestId` is set.

Write failing tests first when implementing this ticket (skill `validation-tdd`). Ship still goes through skill `triple-check`.
