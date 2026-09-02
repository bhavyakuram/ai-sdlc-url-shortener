# Scenario 2: Brownfield — `url-shortener-bulk-shorten`

**Command:** `/run-sdlc java-spring url-shortener-bulk-shorten services-mod --mode=hybrid`
**Full trail:** [`.claude/output/url-shortener-bulk-shorten/`](../../.claude/output/url-shortener-bulk-shorten/)

## Decomposition
Entered directly at **PRE-WORK** (no STEP-0 — `role != greenfield`).
`architecture-analysis`/`codebase-context` did **real** analysis
against the already-built `service-java-spring/` — confirmed the
3-layer dependency direction held, found zero standards drift, and
produced a cache-miss shared-context snapshot (`efe8899`) distinct
from greenfield's clean-slate one. `impact-analysis` confirmed the
enhancement was purely additive — no existing endpoint's shape changes.

## Orchestration
Gates 1/2/3/6 crossed. **Notable event**: `generator` caught its own
sprint-plan mistake mid-implementation — the plan called for adding
`createBulk` to `LinkService`, which would have made the service layer
import an API-layer DTO, violating `rules/architecture.md`'s
`api → service → data` dependency direction. Corrected by keeping
the per-item loop in `LinkController` instead, leaving `LinkService`
untouched. Documented transparently in
[`generator-summary.md`](../../.claude/output/url-shortener-bulk-shorten/step4/generator-summary.md)
rather than silently deviating from the plan.

## Validation
`mvn compile` → BUILD SUCCESS (18→20 source files). The layer-boundary
fix was **re-verified by grep post-build**, not just asserted. 5 new
tests (including a same-request duplicate-alias collision test) +
all 14 prior tests still green → 19/19 PASS, confirming zero
regression. Score **0.95** → **COMPLETE**.

## What This Scenario Demonstrates
Real codebase reasoning (not a clean-slate assumption), and a
governance rule (layer boundaries) actually catching a design smell
during generation — before it became a build failure or a shipped
architectural violation.
