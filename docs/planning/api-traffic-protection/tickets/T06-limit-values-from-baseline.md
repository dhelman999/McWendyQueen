# T06: Set limit values from the ops traffic baseline

- Slug: `api-traffic-protection`
- Size: S
- Depends on: T03 implemented; ops traffic baseline delivered (about a week of
  monitoring, owner: ops)
- PRD: `docs/planning/api-traffic-protection/prd.md`
- Spec: `docs/planning/api-traffic-protection/spec.md`

## Goal

Replace the placeholder limits with values derived from real traffic, so normal
customers never see a 429 while one runaway client is still stopped. This closes
the open limit-value questions in the PRD and spec.

## Acceptance criteria

- [ ] The baseline is recorded in `docs/planning/api-traffic-protection/`: a typical
      week per client address, with peak requests per second and peak burst for
      `general` and for `POST /api/v1/orders`.
- [ ] `general` and `orders` capacity and refill are set with headroom above the
      observed per-client peak. The method and headroom factor are written down.
- [ ] Shared addresses (carrier NAT, office Wi-Fi) are covered: the values fit the
      busiest single address in the baseline.
- [ ] The PRD's open questions on the traffic baseline and the acceptable
      false-positive rate are updated with the outcome.

## Definition of done

- Acceptance criteria all checked.
- Ops has reviewed and agreed to the values; who and when is recorded.
- A replay or load check at baseline peak gives zero 429s, and a single client at
  10x baseline is throttled. The command and output are noted in the PR.
- Change reviewed through skill `triple-check`.

## Likely files

- `src/main/resources/application.yml`
- `docs/planning/api-traffic-protection/baseline.md` (new)
- `docs/planning/api-traffic-protection/prd.md` (open questions)

## Validation (do this before calling the ticket done)

- Full suite: `./mvnw test`
- E2E (manual): the load check in the definition of done, against a running app.

Write failing tests first when implementing this ticket (skill `validation-tdd`). Ship still goes through skill `triple-check`.
