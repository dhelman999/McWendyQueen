# Prime: T06 limit values from the ops baseline

Read `prime-shared.md` first. **Blocked** until T03 is implemented and ops has a
baseline. The data for that baseline is draft ticket T07 (owner TBD).

## Current behavior

- The limits will be T03's generous placeholders in
  `src/main/resources/application.yml`.
- The app cannot produce the baseline today. No access log is configured, and the
  logback pattern carries no client address. Collecting that data is T07, which
  waits on a privacy decision. Do not turn on an access log under this ticket.

## Traps

- Measure per client address, not in aggregate. The method has to surface the
  busiest single address, because shared NAT and office Wi-Fi are the
  false-positive risk.
- Size `orders` on bursts as well as rate. A page that retries a failed order
  (known issue 1: random 500s and 503s) produces bursts from one client.
- No load tool is installed (`hey`, `ab`, `k6`, `wrk` and `jmeter` are all absent).
  Adding one is a dependency decision. A `curl` loop or a small JDK `HttpClient`
  test is enough for "zero 429s at peak, throttled at 10x".
- Edit only the main file. The test values in `src/test/resources/application.yml`
  are pinned in `prime-shared.md`, and the filter tests rely on them.
- Ops sign-off (who and when) is part of the definition of done. An agent cannot
  satisfy it.

## Tests that already cover the area

- T04's filter tests use the test-file values, so changing the production values
  cannot break them.
- T03's main-file binding test reads `src/main/resources/application.yml`. It has
  to keep passing, and to be updated if it asserts specific numbers.

## Suggested first edit

Nothing until the baseline exists. Then write `baseline.md` (method, raw peaks,
headroom factor), edit the main `app.rate-limit` block, update T03's main-file test
if needed, and update the PRD's open questions.
