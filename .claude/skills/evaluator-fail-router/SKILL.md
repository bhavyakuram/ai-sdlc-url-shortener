---
skill_id: evaluator-fail-router
implements_agent: none (framework-level skill, no per-phase agent declaration)
---

# Skill: Evaluator Fail Router

## Purpose
Routes STEP-5 evaluator failures intelligently instead of always cycling STEP-5 to STEP-4

## Implementation Logic
Classifies the failure (missing implementation vs. wrong logic vs. flaky test vs. environment issue) and routes to generator (re-implement), acceptance-criteria (spec was ambiguous), or a flagged-flaky retry accordingly.

## Report Block
Logs its decisions to `.claude/output/{feature-id}/_run-log.md` and,
where it affects cross-run state, to `run-history/_online-learning.yaml`
or `.claude/output/{feature-id}/_token-telemetry.json` as applicable.
