# AI-SDLC Framework — Architecture Overview

**Project:** URL Shortener, built end-to-end by an agentic SDLC
orchestration framework (Agentic-Proficient Software Engineer
assignment). This document is the **Architecture Overview**
deliverable: the framework's static structure and the reasoning behind
it. For the step-by-step run-time flow and a full mapping against the
assignment's own requirements, see
[`docs/aisdlc-flow-and-compliance.md`](aisdlc-flow-and-compliance.md).

## 1. What This Is

An agentic AI-SDLC orchestration framework — agent + skill architecture,
8 HITL gates, a dependency-graph phase pipeline, retry / rollback /
safe-stop, audit-grade telemetry — designed and built from scratch
against the assignment's requirements document, and scoped to a single
service: a URL shortener, buildable in either **Java (Spring Boot)**
or **Python (FastAPI)** via a runtime `stack` argument — no code
duplication between the two, because the framework itself never
hardcodes a language.

## 2. High-Level Architecture

```
OPERATOR (Human-In-The-Loop)
    |
/sdlc-launcher <stack> <feature> [role] [--mode] [--platform]
    |
    v
SDLC LAUNCHER (skills/sdlc-launcher/SKILL.md)  <-- the orchestrator
    | resolves: STACK x ROLE x MODE x PLATFORM
    | validates: stack-validator -> shared-context-bootstrap -> role-resolver
    v
    +---------------+---------------+
    v               v               v
RULES LAYER     SHARED CONTEXT   MCP INTEGRATIONS
(.claude/rules)  (cached arch/   (code-graph; jira/figma/
20 universal     codebase        playwright scaffolded,
enforcement      analysis,        inactive for this project)
files)           content-addressed)
    |
    v
PHASE EXECUTION PIPELINE (see Section 4)
    |
    v
OUTPUT & TELEMETRY (.claude/output/{feature-id}/)
```

The orchestration layer **is** a Claude Code skill
(`skills/sdlc-launcher/SKILL.md`) that dispatches other agents/skills —
not a separately-coded engine. Gates, retries, and audit state live in
plain files (`_role-context.yaml`, `_run-log.md`,
`_reliability-metrics.json`) that the launcher and every dispatched
agent read/write according to `rules/`. This is a deliberate choice:
it keeps the control-plane logic auditable as plain text and lets a
human read exactly what happened in any run without a separate
debugger.

## 3. The Four Configuration Axes

| Axis | Answers | Values for this project |
|---|---|---|
| Stack | What technology are we building with? | `java-spring` \| `python-fastapi` |
| Role | What slice of the feature are we delivering? | `fullstack` (default) \| `services-mod` \| `services-doc` \| `greenfield` |
| Mode | How autonomous is the pipeline? | `deterministic` \| `hybrid` \| `agentic` (default for this project) |
| Platform | What cloud infrastructure? | `none` (default) — no cloud overlay needed for this scope |

## 4. The Phase Pipeline

| Phase | Purpose | Key agents | Gate(s) |
|---|---|---|---|
| STEP-0 (greenfield only) | Concept → prototype | concept-refinement, market-research, ux-prototype (conditional — only if the stack declares a `frontend` layer; not for this project) | 0, 0.5 (0.5 only if ux-prototype fired) |
| PRE-WORK | Establish immutable context every phase reads | triage, code-graph-bootstrap, architecture-analysis, codebase-context, requirement-ingestion, posture-feasibility, role-feasibility-pass1 | — |
| STEP-1 Discovery | Is this feasible? What's the risk? | feasibility, dependency-audit, impact-analysis, risk-analysis, role-feasibility-pass2, git-history-capture | 1 |
| STEP-2 Spec & UX | Define WHAT the feature does | feature-spec, ux-design, acceptance-criteria | 2 |
| STEP-3 Technical Design | Define HOW it's built | technical-design, api-contract, refactor-migration, state-migration | 3, 5, 6, 7\* |
| STEP-4 Planning & Impl | Plan, generate, verify it compiles | planner, generator, build-verdict | — |
| *(retry loop, max 5/role lane)* | STEP-4 ⇄ STEP-5 ⇄ STEP-6 | — | — |
| STEP-5 Validation | Verify the code works | test-generation, coverage-edge, evaluator | — |
| STEP-6 Audit & Grading | Final quality gate | static-analysis, security-audit, grading-feedback | 4 |

\* Gates 5/6/7 collapsed to Gate-3-extension semantics in this
project's smaller gate catalog — see `.claude/reference/mode-catalog.md`.

Greenfield and brownfield share this **same** pipeline: greenfield
enters at STEP-0, brownfield enters directly at PRE-WORK where
`architecture-analysis`/`codebase-context`/`impact-analysis` do real
codebase reasoning against the already-built service. An ambiguous
requirement isn't a third code path — it's the same PRE-WORK entry with
a deliberately thin `prd-v0.md`, which `posture-feasibility` /
`role-feasibility-pass1` are expected to flag at Gate 1 rather than
silently proceeding.

## 5. Agent / Skill Separation

- **Agents** (`agents/**/*.md`) declare WHAT: phase placement,
  input/output contract, required capabilities, model tier.
- **Skills** (`skills/**/SKILL.md`) implement HOW: the concrete
  algorithm, tool usage, recipe calls into the active stack.
- **Rules** (`rules/*.md`) are universal, technology-agnostic law.
- **Stack context** (`stacks/{stack}/`) injects technology-specific
  knowledge (standards to OBEY, reference code to IMITATE, recipes to
  invoke by id).

No agent, skill, or rule file names a stack, file extension, or grep
pattern — see `.claude/rules/architecture.md`.

## 6. Human-in-the-Loop Gates

8 gates (0, 0.5, 1, 2, 3, 4, 5, 6) using `AskUserQuestion`, captured to
structured YAML for audit/reproducibility. Full gate x mode matrix:
`.claude/reference/mode-catalog.md`.

## 7. Retry, Rollback & Safe-Stop

Retry: max 5 per feature per role lane, each retry must address explicit
feedback (`.claude/rules/retry-policy.md`). On exhaustion, or on a
retry that itself regresses previously-passing work, the framework
**rolls back** the current wave to the last known-good commit and
**safe-stops** — surfacing at Gate 4 with full failure history rather
than looping forever or failing silently
(`.claude/rules/retry-rollback-safestop.md`, added specifically to
satisfy the assignment's explicit "bounded retries, fallback, rollback,
and safe-stop controls" requirement).

## 8. Observability & Reliability Metrics

Every run writes, per feature-id:
- `_run-log.md` — append-only audit log (START/END per agent dispatch)
- `_role-context.yaml` — resolved role, policies, gate thresholds
- `_token-telemetry.json` — cost tracking
- `_reliability-metrics.json` — **new**: phase timestamps, retry/rollback
  counts, success rate, MTTR, end-to-end latency
  (`.claude/rules/reliability-metrics.md`)

## 9. Key Design Decisions

1. **Markdown/YAML as the framework's own implementation language** —
   the orchestration control-plane is ~300 Markdown files + ~80 YAML
   files + 2 Python hook scripts. No compiled orchestrator binary,
   so every decision the launcher and its agents make stays
   inspectable as plain text.
2. **techStack as a runtime axis, not a build-time choice** — the same
   agent/skill/rule files drive either `java-spring` or
   `python-fastapi` output; only `stacks/{stack}/` differs.
3. **Agents+skills over a hand-rolled engine** — the control-plane
   bookkeeping that must be deterministic (retry counters, gate
   thresholds, audit log) lives in plain files the launcher
   reads/writes; the SDLC reasoning itself is delegated to Claude Code
   subagents per phase.
4. **Axis catalog sized to the actual scope** — 4 roles, 2 stacks, 1
   active platform (`none`) — because this project is one service, not
   a multi-client mobile/web/backend program. The mechanism (gates,
   retries, rules, audit) is implemented in full regardless of how
   small the catalog is.

## 10. Known Limitations (see also `docs/testing-and-limitations.md`)

- Rollback is git-based (revert the current wave's files) — it does
  not yet handle a rollback that spans a completed database migration.
- `online-learning`'s auto-approval needs 10 consistent decisions per
  gate+matrix-row to activate — this project's run count won't reach
  that threshold, so it stays documented but unexercised in practice.
- MCP integrations beyond `code-graph` are scaffolded but inactive —
  there is no live Jira/Figma workspace for this assignment.
