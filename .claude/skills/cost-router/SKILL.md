---
skill_id: cost-router
implements_agent: none (framework-level skill, no per-phase agent declaration)
---

# Skill: Cost Router

## Purpose
Enforces a hard cost cap of 2x the predicted run cost in agentic mode

## Implementation Logic
Reads config/harness/ model pricing table -> tracks running spend against the STEP-0 predicted budget in _token-telemetry.json -> forces a safe-stop and Gate 4-style operator prompt if spend would exceed the 2x cap.

## Report Block
Logs its decisions to `.claude/output/{feature-id}/_run-log.md` and,
where it affects cross-run state, to `run-history/_online-learning.yaml`
or `.claude/output/{feature-id}/_token-telemetry.json` as applicable.
