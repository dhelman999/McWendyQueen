# T07: Per-client traffic data for the ops baseline (DRAFT)

- Slug: `api-traffic-protection`
- Status: **draft**. Owner TBD. Not ready to implement.
- Size: S (config), plus a privacy decision outside this feature
- Depends on: a privacy decision (the PRD handles privacy separately)
- Blocks: T06 only. It does **not** block T01-T05.
- PRD: `docs/planning/api-traffic-protection/prd.md`
- Spec: `docs/planning/api-traffic-protection/spec.md`

## Goal

Give ops per-client traffic data to build the baseline T06 needs: about a week of
peak requests per second and peak burst per client address, for `general` and
for `POST /api/v1/orders`. Today the app records none of this. No access log is
configured, and `logback-spring.xml` logs no client address.

## Why this is a draft

The obvious mechanism writes customer IP addresses to disk. The PRD lists privacy
as "out of scope for this pass; to be handled separately". So this ticket waits for
that decision, and for an owner, before anyone implements it. It was split out of
T06 on 2026-10-21.

## Candidate approach (not decided)

- Tomcat access log, config only, no new library:
  `server.tomcat.accesslog.enabled`, `pattern`, `directory`, `max-days`
  (property names verified in the Boot 3.5.0 metadata).
- An alternative that may avoid storing addresses: ops measures at whatever sits
  in front of the service, if that keeps logs. The PRD records this as unknown.

## Open questions

- [ ] Owner?
- [ ] Is storing client addresses acceptable, and with what retention? (privacy
      decision, outside this PRD). The same question covers T04's throttle WARN, which
      writes the client address to `logs/application.log` (14-day history). Owner,
      2026-10-22: acceptable for the demo and promotion; a future privacy ticket
      decides it for both.
- [ ] Is the address the app sees the client's own? (PRD assumption; if a proxy
      rewrites it, the baseline would show one "client")
- [ ] Where is the data analyzed, and who produces the T06 numbers from it?

## Acceptance criteria (draft, to confirm when the ticket is picked up)

- [ ] Per-request records with time, client address, method and path, under
      `logs/` with bounded retention.
- [ ] The privacy decision and retention are recorded in this ticket.
- [ ] No change in request behavior, and the full suite passes.

Write failing tests first when implementing this ticket (skill `validation-tdd`). Ship still goes through skill `triple-check`.
