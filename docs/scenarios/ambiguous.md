# Scenario 3: Ambiguous — `url-shortener-analytics-reliability`

**Command:** `/run-sdlc java-spring url-shortener-analytics-reliability services-doc --mode=hybrid`
**Full trail:** [`.claude/output/url-shortener-analytics-reliability/`](../../.claude/output/url-shortener-analytics-reliability/)

## Decomposition
The raw request ([`request.md`](../../.claude/inputs/url-shortener-analytics-reliability/supporting-docs/request.md))
was filed under `services-doc` (audit-only) but *itself* said
"tighten it up if needed" — conditional code-change language. `triage`
recommended `services-mod` instead, directly conflicting with the
filed posture. `posture-feasibility` formally caught this as a
**MISMATCH** — the exact worked example that rule was written around.

## Orchestration — the point of this scenario
Gate 1 extended with role-confirmation options
(RATIFY / EXPAND_LANES / NARROW_LANES / NO-GO) instead of the
framework silently picking a side. The investigation itself (code
reading only, per the filed `doc` posture) found **no bug matching the
original fear** ("quietly losing data" — no such mechanism exists in
the code) but did find one smaller, real, different issue: a
click-write failure would fail the whole redirect, not just the click
count. The operator (not the framework) decided **EXPAND_LANES**,
specifically and only to fix that one finding — logged in
[`_decisions.yaml`](../../.claude/output/url-shortener-analytics-reliability/_decisions.yaml)
along with an explicit reconciliation against `rules/mode-policy.md`'s
"axes frozen for the run" rule (the expansion happens at Gate 1, before
any spec/design/generation work exists — not a mid-flight change after
commitment).

## Validation
The fix (isolate the click-write in a try/catch, not the whole method)
was implemented, and — rather than trusting the try/catch by
inspection — a Mockito test **forces** the click-write to throw and
asserts the redirect still succeeds. 20/20 tests pass. Score **0.95**
→ **COMPLETE**.

## What This Scenario Demonstrates
Requirement Understanding's hardest case: an ambiguity that isn't just
"the spec is vague" but "the filed process itself doesn't match the
request" — surfaced explicitly, decided by a human, and then scoped
tightly to what was actually found rather than opening up unrelated
"improvements."
