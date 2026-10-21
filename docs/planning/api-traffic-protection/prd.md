# PRD: API traffic protection (per-client rate limiting, v1)

- Slug: `api-traffic-protection`
- Status: accepted (2026-10-19, owner review)
- Date: 2026-10-19

## Problem

Restaurant operators and customers using the app are hurt when any single client
sends as many requests as it likes. Nothing stops it today. One badly written
script, or someone intentionally hammering the order endpoint, can slow down the
whole API for everyone - and placing an order is already one of the slower calls.

There is also a fairness problem: one client's volume should not degrade service
for another client.

Cost of not solving it: degradation reaches customers before anyone knows, and the
only remedy is a manual restart.

## Evidence

- Observation (owner): any single client can send unlimited requests; the order
  endpoint is already one of the slowest calls.
- Observation (owner): the team copes by watching and restarting, so problems are
  found only after customers are affected.
- Incident history (how often, how bad, traced to which kind of client):
  **TBD — needs validation.**
- Normal traffic baseline (a typical week, per client and per endpoint):
  **TBD — needs validation** with the ops team.

## Thesis

Part of the API is about to open to the public for a promotion, so traffic will be
much less predictable than today. Watch-and-restart is reactive and only works after
customers have been hurt. A per-client limit acts before the degradation spreads:
the noisy client is slowed, everyone else is not.

## Hypothesis

We believe a per-client request limit on the `/v1` API, enforced inside the app,
will cause a single runaway or abusive client to be throttled with a "too many
requests" response instead of slowing the API for everyone, resulting in restaurant
operators and customers getting consistent response times - including on order
placement - during the public promotion.

- Right: during the promotion window, no slowdown or manual restart is traced to a
  single client, and ops can tune limits without a developer.
- Wrong: any of -
  - normal customers receive "too many requests" responses (for example people
    who share one source address on shared Wi-Fi or a mobile carrier);
  - the API still slows down or needs a restart because of one client's traffic;
  - ops cannot find a limit that stops bad clients without stopping real ones.

## Who

- Operates: the ops team. They set the limits and are paged when something goes
  wrong.
- Benefits: customers using the app (it stays fast) and restaurant operators.
- Not for: normal users - someone ordering lunch should never notice it. It is not a
  replacement for logins or security; it is about volume.

## MVP

A rate limit per client that lives inside this application, on the versioned `/v1`
endpoints. A client is identified by its source address. When a single client goes
over its limit it receives a standard "too many requests" response telling it when
to try again. Limits are configurable by ops.

## Success criteria

- No slowdown or manual restart during the promotion window is traced to a single
  client. Measurement method: **TBD — needs validation** (see open questions).
- A client that exceeds its limit receives "too many requests" with a retry hint;
  other clients' requests keep succeeding at normal speed while that happens.
- Normal customer traffic never receives a "too many requests" response.
  Acceptable false-positive rate: **TBD — needs validation.**
- Ops changes a limit without a developer change.

## Non-goals

- Large-scale distributed denial of service (a "hacker swarm"). Out of scope; v1
  covers an occasional bad actor or runaway script.
- Sharing limits across several servers.
- Tenant awareness, user logins, accounts, or different limits for different
  customers. Can be revisited later if needed.
- Authentication or security controls. This is volume protection only.
- Endpoints outside the versioned `/v1` API.

## Constraints

- No new infrastructure for v1: no new paid services and no new libraries.
- Ops must be able to change limits without waiting for a developer.
- Must be invisible to normal usage.

## Open questions

Answered in the 2026-10-19 review unless still marked open.

- [x] **What sits in front of the service?** Unknown; probably a load balancer.
      The production network path is out of scope for v1. **Assumption:** the
      source address the app sees is the client's own. **Risk carried:** if a load
      balancer or proxy rewrites it, every client shares one limit (see the
      falsifiers). The spec must say how the client address is read.
- [ ] **Traffic baseline**: no number yet. Ops will supply it after about a week
      of monitoring. Limit values wait on it. Owner: ops.
- [ ] **Launch date**: TBD.
- [ ] **Shared source addresses**: carried into the spec. Recommended: accept it as a
      known v1 limitation, with a generous general limit, a tighter order
      limit and throttle logs (option C on the review page). The owner has not chosen yet.
- [x] **"Without waiting for a developer"**: v1 = config edit plus restart.
      Changing limits while the service runs is a wanted follow-up, not v1.
- [x] **Visibility**: v1 = logs only.
- [x] **One limit or several**: order placement (`POST /api/v1/orders`) is the
      slowest call and the one most worth protecting, so it gets its own limit.
- [x] **Exemptions**: API docs, admin routes and health checks are not limited.
- [x] **Measurement window**: the promotion window. It follows the launch date,
      so it stays open until that is set.
- [x] **Single instance**: believed to be one instance. Multi-instance limits stay
      out of scope for v1.
- [x] **Privacy**: out of scope for this pass; to be handled separately.
