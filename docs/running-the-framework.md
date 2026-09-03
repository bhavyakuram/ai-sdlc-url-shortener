# Running the AI-SDLC Framework — Complete Guide

This is the operational reference for actually running this framework
— every way to invoke it, every option, and worked examples drawn
from the three scenarios this project has actually completed.

## 1. Prerequisites

- **Claude Code CLI**, with this repository open as the project
  directory (so `.claude/skills/sdlc-launcher/SKILL.md` is
  auto-discovered as the `/sdlc-launcher` command).
- **A JDK and Maven** for the `java-spring` stack. This project does
  not require them on `PATH` — see [§6 Toolchain Notes](#6-toolchain-notes)
  if you only have an IDE-managed JDK/Maven (e.g. IntelliJ), which is
  the exact situation this project's own runs were built against.
- **Network access** to Maven Central for first-time dependency
  resolution (a few dependencies — Bucket4j, MaxMind GeoIP2 — aren't
  always pre-cached).

## 2. The Entry Point

```bash
/sdlc-launcher <stack> <feature-id> [role] [--mode=deterministic|hybrid|agentic] [--platform=none|aws|gcp|azure]
```

Type this directly in a Claude Code chat. It is a real, registered
skill (Claude Code auto-discovers every `.claude/skills/*/SKILL.md`)
— not a documentation convention. Typing it dispatches the actual
orchestration sequence described in `docs/aisdlc-flow-and-compliance.md`.

### Positional / required arguments

| Argument | Required | Values | Notes |
|---|---|---|---|
| `<stack>` | yes | `java-spring` (only stack with a full implementation in this project; `python-fastapi` is scaffolded but not implemented) | Frozen for the whole run |
| `<feature-id>` | yes | any identifier, e.g. `url-shortener-core` | Must have a matching input under `.claude/inputs/<feature-id>/` before you run — see §4 |
| `[role]` | optional, but effectively required | `greenfield` \| `services-mod` \| `services-doc` | Defaults per `roles/` if omitted; always pass it explicitly — see §3 |

### Flags

| Flag | Values | Default | Effect |
|---|---|---|---|
| `--mode` | `deterministic` \| `hybrid` \| `agentic` | `agentic` (this project's default, per `modes/agentic/mode-manifest.md`) | See §5 for what actually changes |
| `--platform` | `none` \| `aws` \| `gcp` \| `azure` | `none` | No cloud overlay is implemented beyond `none` in this project |

## 3. The Four Ways to Run It — by Role

The role you pass is the single biggest thing that changes what
happens. This project has real, completed examples of three of the
four.

### 3.1 Greenfield — build something new from scratch

```bash
/sdlc-launcher java-spring url-shortener-core greenfield --mode=agentic
```

- **Only role that triggers STEP-0** (concept → market research →
  prototype) before PRE-WORK.
- Entry point expects a raw idea at
  `.claude/inputs/<feature-id>/ideation/idea.md`.
- Gates you'll see: 0, (0.5 only if the stack has a `frontend` layer
  — `java-spring` doesn't, so this project's runs never hit 0.5), 1, 2, 3, 6.
- **Real example**: `url-shortener-core` — see
  [`docs/scenarios/greenfield.md`](scenarios/greenfield.md). Ran a
  genuine 3-way parallel design exploration at STEP-3, hit one real
  build failure + retry, scored 0.985.

### 3.2 Brownfield enhancement — add to existing code

```bash
/sdlc-launcher java-spring url-shortener-bulk-shorten services-mod --mode=agentic
```

- Enters directly at **PRE-WORK** — `architecture-analysis`/
  `codebase-context` read your *actual* existing code, not a
  clean-slate assumption.
- Input expected at
  `.claude/inputs/<feature-id>/supporting-docs/<anything>.md` (a
  ticket export, a written request — anything descriptive).
- Gates: 1, 2, 3, 6 (no STEP-0, so no Gate 0/0.5).
- **Real example**: `url-shortener-bulk-shorten` — see
  [`docs/scenarios/brownfield.md`](scenarios/brownfield.md). Found and
  closed two real risks (a rate-limit amplification vector, a Spring
  AOP self-invocation bug) before any code was written. Zero retries,
  scored 0.985.

### 3.3 Audit / documentation-only — no code changes

```bash
/sdlc-launcher java-spring url-shortener-analytics-reliability services-doc --mode=agentic
```

- Filed posture is **doc** — `agents_skipped: [generator, test-generation]`
  per `roles/services-doc/role-manifest.md`. In principle, this
  produces an audit report with **no code change**.
- **The interesting case**: if the raw request's own wording implies
  a fix is needed, `posture-feasibility` flags a **MISMATCH** at
  Gate 1, and the gate extends with role-confirmation options:
  `RATIFY` (stay doc-only) / `EXPAND_LANES` (widen to `services-mod`
  and actually fix it) / `NARROW_LANES` (not applicable from `doc`,
  already the narrowest) / `NO-GO`.
- **Real example**: `url-shortener-analytics-reliability` — see
  [`docs/scenarios/ambiguous.md`](scenarios/ambiguous.md). Filed as
  `doc`, the investigation found a real (different, more precise)
  defect, and the operator chose `EXPAND_LANES` at Gate 1 — the role
  changed **mid-run**, before any spec/design work existed.

## 4. Preparing Input Before You Run

Every run reads its raw ask from `.claude/inputs/<feature-id>/`
(`rules/input-path.md`) — create this **before** invoking
`/sdlc-launcher`:

```
.claude/inputs/<feature-id>/
├── ideation/idea.md          <- greenfield only: the raw idea, as loose as you like
├── supporting-docs/*.md      <- brownfield/ambiguous: a request, ticket export, anything descriptive
└── jira/                     <- scaffolded, inactive (no live Jira workspace in this project)
```

**Example — the actual greenfield input used in this project**
(`.claude/inputs/url-shortener-core/ideation/idea.md`, abridged):
> "We need a URL shortener service, built from scratch... Not sure yet
> whether expiry should be a fixed TTL for everyone or configurable
> per-link, and analytics could mean anything from 'just a counter' to
> 'full click log' — figure out what's reasonable... and flag anything
> that needs a decision."

Deliberately loose, on purpose — `concept-refinement` is designed to
resolve exactly this kind of ambiguity, not to be handed a pre-cleaned
spec.

## 5. Mode Options — What Actually Changes

| | `deterministic` | `hybrid` | `agentic` (this project's default) |
|---|---|---|---|
| Who decides "what's next" | Fixed rule only | + 6 bounded decision surfaces (triage, risk-weighting, coverage-tier, fail-routing, confidence-scoring, retry-escalation) | + `conductor` sequences phases itself, within a `transition-fsm` |
| Exploring design alternatives | Never | Never | `parallel-explorer` can dispatch up to 3 real concurrent subagents for a genuine fork |
| Retry budget | Fixed at 5 | Fixed at 5 | `adaptive-gate` can calibrate 3-8 from history (never triggered yet in this project — no matrix row has enough history) |
| Cost control | None | None | `cost-router` hard-caps at 2x predicted spend (not exercised — no real token metering in this environment) |
| Gates 4 & 5 | Always manual | Always manual | `online-learning` can auto-approve after 10 consistent identical decisions (never triggered — nowhere near that threshold) |
| Gates 0-3, 6 | Always manual | Always manual | **Still always manual** — this never changes, in any mode |

All three of this project's completed runs used `--mode=agentic`
explicitly (matching the project default). To reproduce the *original*
hybrid-mode dry runs referenced in git history, pass `--mode=hybrid`
— the mechanism still works, just without `conductor`/`parallel-explorer`/etc.

## 6. What Happens During a Run

1. You type the command. The launcher resolves the 4 axes and logs
   the start to `.claude/output/<feature-id>/_run-log.md`.
2. Real subagents are dispatched per phase (visible as `Agent` tool
   calls) — each one reads the actual approved artifacts from prior
   phases and the actual codebase, not an assumption.
3. **You will be asked to make real decisions** via a multiple-choice
   prompt at every gate — this is not a rubber-stamp; read the summary
   each gate presents (it includes the specific findings that phase
   produced) before answering.
4. On a build/test failure, you'll see a real compiler/test error
   attached, and the run retries automatically (up to 5 times per role
   lane) — you don't need to do anything unless it exhausts the budget
   (Gate 4 waiver decision).
5. On `COMPLETE`, check `.claude/output/<feature-id>/step6/grading-report.md`
   for the final score and `service-java-spring/` for the actual code.

## 7. Inspecting a Run Afterward

Every run leaves a complete, inspectable trail:

```
.claude/output/<feature-id>/
├── step0/ .. step6/           <- every phase's actual artifacts (specs, designs, reports)
├── _role-context.yaml         <- resolved role/policies/gate thresholds for this run
├── _run-log.md                <- append-only, human-readable narrative of everything that happened
├── _decisions.yaml            <- every gate decision, structured (reproducible)
├── _token-telemetry.json      <- cost tracking (not populated with real numbers in this project — see limitations)
└── _reliability-metrics.json  <- real success_rate/retry_frequency computed from actual phase outcomes
```

Cross-run learning lives at `.claude/run-history/_online-learning.yaml`
— per stack+role "matrix row," across every run of that combination.

## 8. Re-running / Starting Fresh

To genuinely re-trigger a feature-id from scratch (not resume):

```bash
rm -rf .claude/output/<feature-id>
rm -rf .claude/shared-context/<stack>/snapshots/*   # forces a fresh codebase re-analysis
# leave .claude/inputs/<feature-id>/ alone — the raw ask doesn't need to change
```

Then invoke `/sdlc-launcher` again with the same arguments. This
project did exactly this to move from an earlier hybrid-mode pass to
the agentic-mode runs actually reported in `docs/scenarios/`.

## 9. Building/Testing the Generated Service Directly (outside the framework)

Once a run has completed, `service-java-spring/` is a normal Maven
project:

```bash
cd service-java-spring
mvn test     # runs the full suite + JaCoCo coverage report at target/site/jacoco/
mvn spring-boot:run   # starts the service on :8080
```

If `mvn`/`java` aren't on `PATH`, see §10.

## 10. Toolchain Notes (this environment specifically)

This project's own runs were built on a machine with **no JDK/Maven
on `PATH`** — only an IntelliJ-managed JDK and IntelliJ's bundled
Maven. If you hit `'mvn' is not recognized`, locate and export them
for your session, e.g. (Windows/Git Bash):

```bash
export JAVA_HOME="$HOME/.jdks/openjdk-19"   # adjust to your actual IDE-managed JDK path
export PATH="$JAVA_HOME/bin:/c/Program Files/JetBrains/IntelliJ IDEA Community Edition <version>/plugins/maven/lib/maven3/bin:$PATH"
```

Two related things this project's own build hit, worth knowing:
- **First build must be online** (drop `-o`) to resolve dependencies
  not already cached locally (e.g. a specific H2 or Bucket4j version).
  Subsequent builds can use `-o` (offline) once cached.
- **Always verify with a repeated `mvn clean test`**, not just one
  green run — this project found a real test-isolation bug
  (`H2FileModeDurabilityTest` silently hitting a shared file instead
  of its intended isolated temp directory) specifically because a
  second run was checked. See `docs/testing-and-limitations.md`.

## 11. Quick Reference — All Three Real Commands Used in This Project

```bash
# Greenfield
/sdlc-launcher java-spring url-shortener-core greenfield --mode=agentic

# Brownfield
/sdlc-launcher java-spring url-shortener-bulk-shorten services-mod --mode=agentic

# Ambiguous
/sdlc-launcher java-spring url-shortener-analytics-reliability services-doc --mode=agentic
```

Each reached `COMPLETE` with a grading score of 0.985. Full narrative
of what actually happened in each: [`docs/scenarios/`](scenarios/).
