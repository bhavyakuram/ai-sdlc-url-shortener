# Scenario 3: Ambiguous — `url-shortener-analytics-reliability` (agentic mode, real trigger)

**Triggered directly**: `/sdlc-launcher java-spring url-shortener-analytics-reliability services-doc --mode=agentic`
**Full trail:** [`.claude/output/url-shortener-analytics-reliability/`](../../.claude/output/url-shortener-analytics-reliability/)

## The setup, verified authentic before dispatch
The raw request ([`request.md`](../../.claude/inputs/url-shortener-analytics-reliability/supporting-docs/request.md))
is filed `services-doc` (audit-only) but its own wording — "tighten it
up if needed" — is conditional code-change language. Before dispatching
anything, the conductor confirmed the click-write in the *current*
`LinkService.java` genuinely had no failure isolation yet (deliberately
never re-applied in the fresh greenfield/brownfield builds), so this
run's investigation is real, not staged.

## `posture-feasibility` caught the mismatch — and the investigation found something *more precise* than the original fear
`triage` independently recommended `services-mod`, conflicting with
the filed `services-doc`. The investigation (reading the real code
fresh) found: **not silent data loss** (a failure is logged and
returns `500` — loud, not quiet) — but a real, different, fixable
defect: **availability coupling**. A transient analytics-write hiccup
currently fails the user's redirect too, and the existing rate limiter
doesn't even protect against a viral-link burst (it's scoped per
IP+code, not total concurrent traffic). Gate 1 extended with
RATIFY/EXPAND_LANES/NO-GO; the operator chose **EXPAND_LANES**,
specifically and only for this finding.

## The fix had a real, easy-to-miss correctness trap
The obvious fix — wrap the click-write in try/catch — has a subtle
bug if implemented with `save()` instead of `saveAndFlush()`:
`@Transactional`'s AOP proxy defers the physical `INSERT` to
transaction-commit time, *after* the method returns — so a try/catch
around a plain `save()` would catch nothing for the real failure mode
(lock timeout, pool exhaustion under a burst). This was caught at
design review (STEP-3), not left for a production incident to surface.
`@Async` was considered and rejected on 3 independent grounds
(no async infra exists; would reintroduce the exact self-invocation
risk the brownfield run already fixed once; would silently change the
tested immediate-consistency behavior).

## A retry that was the conductor's own mistake, accounted for the same way
The first build attempt failed — not a design flaw, but a bug in the
conductor's own new unit test (a bare `ShortLinkEntity` never gets a
JPA-managed `id`, and `ClickEventEntity` requires one). Fixed and
logged with the same honesty applied to every subagent's mistakes in
this project — no exemption for "the orchestrator did it."

## Validation
Forced-failure test's own log output proves the fix actually works,
not just that the test passed: *"Failed to record click for code
'fail-test' ... redirect will proceed without recording this click."*
53/53 tests pass (3 new + 50 pre-existing, zero regression), verified
with 2 consecutive `mvn clean test` runs. 91.6% coverage.
`grading-feedback` scored **0.985** → **COMPLETE**.

## What This Scenario Demonstrates
The hardest case for "Requirement Understanding": an ambiguity that
isn't just a vague spec but a **mismatch between the filed process
and the request itself** — surfaced explicitly rather than silently
resolved either way, investigated for what's *actually* true rather
than confirming the original fear, and fixed with a design review
rigorous enough to catch a real correctness trap before it shipped.
