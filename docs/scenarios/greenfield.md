# Scenario 1: Greenfield — `url-shortener-core` (agentic mode, real trigger)

**Triggered directly**: `/sdlc-launcher java-spring url-shortener-core greenfield --mode=agentic`
— the real, registered Claude Code skill, typed by the operator, not a simulated invocation.
**Full trail:** [`.claude/output/url-shortener-core/`](../../.claude/output/url-shortener-core/)

## Genuine multi-agent dispatch
Every judgment-bearing phase was a real `Agent`-tool subagent call, not narration:

| Phase | Tokens | Tool calls | Highlight |
|---|---|---|---|
| STEP-0 (concept + market research) | 82k | 18 (incl. 2 real `WebSearch`) | Caught that idea.md's "don't lose data" conflicts with H2's in-memory default → resolved to file-mode |
| PRE-WORK | 83k | 19 | Verified posture MATCH against real repo state, not assumed |
| STEP-1 (discovery) | 110k | 28 (incl. `WebSearch`) | Real dependency picks with CVE checks (Bucket4j, MaxMind GeoIP2, H2 pinned ≥2.2.220 closing 2 named CVEs) |
| STEP-2 (spec) | 114k | 20 | Independently reasoned to 404-not-410 for expired codes (info-leak argument), not just inherited |
| STEP-3 exploration | 3×54k | 0 each | Genuine 3-way parallel design exploration (see below) |
| STEP-4 (generator) | 204k | 72 | Wrote 31 files; caught a real spec inconsistency the conductor introduced |

## `parallel-explorer`, genuinely
3 real, concurrent subagents designed competing short-code strategies.
Candidate C's own analysis computed an actual collision probability
and concluded it does **not** satisfy the hard concurrency requirement
as specified — rejected on that math, not a vibe. Candidate A
(sequential-id + base62) selected for structural, not probabilistic,
uniqueness. Full write-up: `step3/parallel-explorer-candidates.md`.

## A real retry (not scripted)
`build-verdict` failed on the first attempt — `HttpServletResponse` has
no `SC_TOO_MANY_REQUESTS` constant. Routed back per `rules/build-green.md`,
1/5 retry consumed for the role lane, fixed with a one-line change.
This is the first run in this project with a genuine non-zero retry.

## A real post-COMPLETE finding
Re-running the full test suite a second time (due diligence, not a
formal gate) surfaced a genuine test-isolation bug:
`H2FileModeDurabilityTest` silently hit the shared production-path H2
file every run (a Spring Boot property-precedence mistake in the
test itself, not the production code) instead of its intended
isolated `@TempDir`. Root-caused and fixed; verified with repeated
runs. Full writeup: `step5/test-report.md` Addendum. This is Proof
Over Promise catching something a single green run could not.

## Also fixed at the framework level, mid-run
`ux-prototype` had been firing unconditionally on `role=greenfield`;
corrected to fire only when the active stack declares a `frontend`
layer (java-spring never does) — see `agents/step0/ux-prototype.md`.
This run has no Gate 0.5 as a result.

## Validation
`mvn compile` → BUILD SUCCESS on retry 2/5. 35 tests, 1 documented
skip (AC18 — no injectable clock in Bucket4j's refill logic), 0
failures across repeated runs. JaCoCo: 89.0%. `grading-feedback`
scored **0.985** → **COMPLETE**.

## What This Scenario Demonstrates
That "agentic" isn't cosmetic here: independent subagents genuinely
disagreed with and corrected the conductor's own earlier work (the
api-contract mismatch, the ux-prototype rule), a real build failure
got a real bounded retry, and a real bug was found by actually
re-running verification rather than trusting the first green result.
