# Testing Approach, Limitations & Trade-offs

Based on 3 completed runs through the framework:
[`url-shortener-core`](scenarios/greenfield.md) (greenfield),
[`url-shortener-bulk-shorten`](scenarios/brownfield.md) (brownfield),
[`url-shortener-analytics-reliability`](scenarios/ambiguous.md) (ambiguous).

## Testing Approach
- **Framework mechanism**: every HITL gate (0, 0.5, 1 incl. its
  role-confirmation extension, 2, 3, 6) was exercised at least once
  across the three scenarios with a real operator decision, not a
  simulated one.
- **Product tests**: 20 real JUnit tests in
  [`service-java-spring/src/test/`](../service-java-spring/src/test/java/com/aisdlc/urlshortener/),
  run via `mvn -o test` after every generation wave across all three
  runs — never asserted from reading the generated code.
- **AC traceability**: all 16 acceptance criteria (AC-1..AC-16 across
  the three runs) map to at least one test — see each run's
  `step5/test-report.md`.

## Known Limitations
1. **No JaCoCo line-coverage percentage.** `rules/testing.md`'s 80%
   coverage floor is verified qualitatively (AC-traceability) across
   all three runs, not mechanically measured. This is the single
   largest, consistently-flagged gap in every `grading-report.md`.
2. **No HTTP-layer test for the 410-expiry path**
   (`url-shortener-core`) — the behavior is proven at the service
   layer; asserting the actual HTTP status needs a mockable clock,
   which wasn't part of any of the three approved designs.
3. **`shared-context` snapshot keying evolved mid-project.** The first
   two runs used the git HEAD SHA as a stand-in for the spec's
   "content hash of tracked file hashes"; the third run switched to a
   real md5 of concatenated source bytes (correctly capturing
   uncommitted changes the git-SHA approach would have missed). Both
   are documented in their respective `manifest.json` files — flagged
   as an evolving-fidelity gap, not backfilled onto the earlier runs.
4. **No static-analysis tooling wired into `pom.xml`** (spotbugs/
   checkstyle) — every `static-analysis-report.md` used direct `grep`
   checks instead, documented as a gap in each report rather than
   asserting a clean tool run that never happened.
5. **`agentic` mode was never exercised.** All three runs used
   `hybrid` mode; `conductor`/`parallel-explorer`/`online-learning`
   remain scaffolded but unproven in practice.
6. **`_token-telemetry.json` / `_reliability-metrics.json` are not
   populated with real numbers** for any of the three runs — these
   were driven manually rather than through Claude Code's own `Agent`
   tool dispatch, so there was no per-agent token metering to record.
   A true `/run-sdlc` execution would populate these from real usage.

## Trade-offs
- Chose to run this framework's mechanism *manually* (walking the
  launcher sequence myself, dispatching each phase's real work,
  pausing at every gate with `AskUserQuestion`) rather than building
  the launcher as literal executable orchestration code first. This
  proved the mechanism end-to-end across all three required scenarios
  within the assignment's time budget, at the cost of the
  `_token-telemetry.json`/`_reliability-metrics.json` gaps above.
- The brownfield run's generator-time layer-boundary catch
  (`url-shortener-bulk-shorten/step4/generator-summary.md`) was
  resolved by adjusting *where* code lived, not by looping back
  through a full Gate-3 re-approval — judged proportionate since the
  approved behavior/contract didn't change, only an internal
  implementation-placement detail the plan hadn't actually mandated.
- The ambiguous run's Gate 1 role expansion (`services-doc` →
  `services-mod`) is reconciled against `rules/mode-policy.md`'s
  "axes frozen for the run" rule by treating Gate 1 as still within
  "run start," before any spec/design/generation investment exists —
  documented explicitly in that run's `_decisions.yaml` as an
  interpretation, not asserted as unambiguously correct.
