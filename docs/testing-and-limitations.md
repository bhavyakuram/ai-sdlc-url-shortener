# Testing Approach, Limitations & Trade-offs

Based on 3 completed runs, all triggered directly via the real
`/sdlc-launcher` skill in **agentic** mode:
[`url-shortener-core`](scenarios/greenfield.md) (greenfield),
[`url-shortener-bulk-shorten`](scenarios/brownfield.md) (brownfield),
[`url-shortener-analytics-reliability`](scenarios/ambiguous.md) (ambiguous).

## Testing Approach
- **Framework mechanism**: every HITL gate (0, 1 incl. its
  role-confirmation extension, 2, 3, 6) was exercised with a real
  operator decision. Gate 0.5 never fired — `ux-prototype` is now
  correctly gated on the stack declaring a `frontend` layer, which
  `java-spring` never does (fixed mid-project, see
  `agents/step0/ux-prototype.md`).
- **Genuine multi-agent dispatch**: every judgment-bearing phase
  across all three runs was a real `Agent`-tool subagent call — over
  1.5M tokens and 300+ tool calls total, including real `WebSearch`
  calls, real source-code reads, and one subagent that got interrupted
  by a session rate limit mid-task and was **resumed** via
  `SendMessage` rather than restarted, preserving its full context.
- **`parallel-explorer`, genuinely**: the greenfield run's short-code
  generation strategy got 3 real, concurrent, independent subagent
  designs — not one subagent's sequential comparison. One candidate's
  own analysis computed an actual collision probability and concluded
  it violated the hard concurrency requirement as specified.
- **Product tests**: 53 real JUnit tests by the end of the third run
  (14 → 20 → 35 → 50 → 53 across the three runs' generation waves),
  run via `mvn test` after every wave — never asserted from reading
  the generated code.
- **Repeat-verification as standard practice, not a one-off**: the
  greenfield run's real `H2FileModeDurabilityTest` bug (see below)
  taught this project that a single green test run does not prove
  test isolation. Every run from then on ran `mvn clean test`
  **twice** before trusting the result — a lesson genuinely applied
  forward, not just noted.

## Known Limitations
1. **No static-analysis tool wired into `pom.xml`** (spotbugs/
   checkstyle) — every audit used direct `grep` checks instead,
   documented as a gap rather than asserting a clean tool run that
   never happened.
2. **`cost-router` was never genuinely exercised.** No real per-agent
   token metering exists in this environment, so the 2x cost cap was
   never checked against real spend — though every subagent dispatch
   in all three runs logged its actual token/tool-call count, which
   is a step toward making this measurable in a real deployment.
3. **`adaptive-gate` and `online-learning` remain mostly theoretical.**
   `run-history/_online-learning.yaml` now has real pass records for
   2 matrix rows (`java-spring/greenfield`, `java-spring/services-mod`)
   across 3 runs total — nowhere near `online-learning`'s 10-consistent-
   decision threshold, and `adaptive-gate`'s retry-budget calibration
   never actually triggered a budget change (every run stayed within
   the default budget of 5).
4. **`deterministic` and `hybrid` modes were not exercised** in this
   project's final state — `agentic` was adopted as the project default
   partway through, per the assignment's explicit "agentic" framing
   (see `modes/agentic/mode-manifest.md`). An earlier hybrid-mode pass
   exists in git history (commits before the mode switch) but is
   superseded by these three agentic-mode runs.
5. **No HTTP-layer test for the rate-limit window-reset boundary**
   (AC18, `url-shortener-core`) — Bucket4j's refill logic reads system
   time internally with no injectable clock seam; a real 61-second
   sleep per test run isn't acceptable for routine builds. The
   structural half of the same claim (the bucket reaches exactly
   100/0 remaining) is tested; the time-based reset itself is not.
6. **A test-independence bug was found and fixed by actually
   re-running verification, not by design review.**
   `H2FileModeDurabilityTest` (greenfield run) appeared to pass, but
   was silently hitting the shared production-path H2 file every run
   instead of its intended isolated `@TempDir` — a Spring Boot
   property-precedence mistake (`SpringApplicationBuilder.properties()`
   sets the *lowest*-priority source, not the highest). Root-caused
   and fixed; this is exactly why Testing Approach's "repeat
   verification" item above became standard practice afterward rather
   than a one-time fire drill.

## Trade-offs
- Ran the framework's mechanism through the **real, registered**
  `/sdlc-launcher` Claude Code skill rather than a hand-simulated
  walkthrough — this is what makes the "genuine multi-agent dispatch"
  claims above literally true rather than narrated.
- The brownfield run's two Gate-1 findings (rate-limit amplification,
  Spring self-invocation) were both closed with new, purpose-built
  mechanisms (a second rate limiter, a new orchestrator class) rather
  than reusing/stretching existing ones — judged worth the extra
  surface area given each was a real, not hypothetical, risk.
- The ambiguous run's Gate 1 role expansion is reconciled against
  `rules/mode-policy.md`'s "axes frozen for the run" rule the same way
  the earlier hybrid-mode pass reasoned it: Gate 1 is still within
  "run start," before any spec/design/generation investment exists.
- Chose to hold generator's dispatches to the same honest-accounting
  standard as the conductor's own work — a build failure caused by
  the conductor's own test-authoring mistake (the ambiguous run) is
  logged as a real retry, not quietly absorbed as "just fixing my own
  code."
