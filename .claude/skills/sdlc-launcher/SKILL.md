---
name: sdlc-launcher
skill_id: sdlc-launcher
implements_agent: none (the entry point — orchestrates all other agents)
description: >
  THE entry point for the AI-SDLC framework. Invoke as
  /sdlc-launcher <stack> <feature-id> [role] [--mode=deterministic|hybrid|agentic] [--platform=none|aws|gcp|azure]
  — e.g. "/sdlc-launcher java-spring url-shortener-core greenfield --mode=agentic".
  Resolves the four configuration axes (stack/role/mode/platform),
  bootstraps shared context, then dispatches every phase agent
  (STEP-0..STEP-6) in order, pausing at each HITL gate for a real
  operator decision. Use this whenever asked to run/start/launch a
  feature through the framework, or to continue/resume one that's
  in progress.
---

# Skill: SDLC Launcher

**THE entry point.** Resolves the four configuration axes, dispatches
every phase agent, tracks state, and is the only skill permitted to
write `.claude/output/{feature-id}/_run-log.md` START/END framing
entries at the run level (individual agents append their own entries
inside that framing).

## Launcher Orchestration Sequence

1. **Parse arguments** (stack, feature, role, mode, platform, phase flags)
2. **Validate stack** → `skills/stack-validator/SKILL.md`
   1. Tier 1: structural (manifest exists, capabilities valid)
   2. Tier 2: references (`stack-skills.yaml` paths resolve, standards
      inheritance valid)
   3. Tier 3: depth (reference freshness, deprecation cross-check)
3. **Bootstrap shared context** → `skills/shared-context-bootstrap/SKILL.md`
   1. Compute content hash per repo state
   2. Cache hit → reuse snapshot. Miss → regenerate.
   3. Data-context ingestion if `db-harness/` is configured
4. **Resolve role** → `skills/role-resolver/SKILL.md`
   1. Load role manifest
   2. Resolve `quality_policies` → `_role-context.yaml.policies`
   3. Resolve gate thresholds
   4. Write `_role-context.yaml`
5. **Resolve platform** → `skills/platform-resolver/SKILL.md`
6. **Validate mode** → `rules/mode-policy.md` step 1c
7. **Cross-check capabilities** (what agents require vs. what the stack
   manifest provides — reject at launch if any agent's `Requires`
   contract can't be satisfied)
8. **Dispatch phase agents** with the canonical shared-context block
   (`rules/prompt-caching.md` byte-identical ordering)
9. **Manage HITL gates** (pause via `AskUserQuestion`, capture the
   decision into a structured YAML for audit/reproducibility, resume)
10. **Track retries** — per role lane, max 5 (`rules/retry-policy.md`),
    invoking `rules/retry-rollback-safestop.md` on exhaustion
11. **Write audit log** — `_run-log.md` START + END entries per agent
    dispatch, plus rollback/safe-stop events
12. **Update telemetry** — `_token-telemetry.json` (cost) AND
    `_reliability-metrics.json` (phase timing, retry/rollback counts,
    success rate, MTTR, end-to-end latency — `rules/reliability-metrics.md`)

## Dispatch Mechanics
Each phase agent is dispatched as a Claude Code subagent via the
`Agent` tool with a typed prompt: the agent's own `.md` contract +
its `SKILL.md` implementation + the canonical shared-context block.
Parallel-eligible agents within a stage (e.g. STEP-1 Stage 1:
`feasibility` + `dependency-audit`) are dispatched in the same
message, not sequentially.

## Determining Entry Phase (greenfield vs. brownfield vs. ambiguous)
- `role=greenfield` (or `triage` classifies `feature_shape=greenfield-app`
  from an empty/near-empty repo) → enter at **STEP-0**.
- Any other role against an existing repo → enter at **PRE-WORK**
  directly; `architecture-analysis`/`codebase-context` do real
  brownfield codebase reasoning instead of declaring a clean-slate
  baseline.
- An ambiguous/thin requirement is not a different entry point — it is
  the same PRE-WORK entry, where `requirement-ingestion` produces a
  necessarily thin `prd-v0.md`, and `posture-feasibility` /
  `role-feasibility-pass1` are expected to surface a warning at Gate 1
  rather than a clean GO. This is the framework demonstrating its own
  ambiguity-handling, not a special-cased code path.

## Report Block
Prints a run summary on COMPLETE or SAFE-STOPPED: final grading score,
phases run, gates crossed, retries/rollbacks consumed, and the path to
`_reliability-metrics.json` for the full numbers.
