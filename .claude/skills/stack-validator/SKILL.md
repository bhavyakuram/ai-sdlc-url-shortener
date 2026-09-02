---
skill_id: stack-validator
implements_agent: none (framework-level skill, no per-phase agent declaration)
---

# Skill: Stack Validator

## Purpose
Pre-flight validation of the resolved stack config before any agent is dispatched

## Implementation Logic
Tier 1 structural (manifest exists, capabilities valid) -> Tier 2 references (paths resolve, standards inheritance valid) -> Tier 3 depth (reference freshness, deprecation cross-check). Aborts the run with a clear error if any tier fails.

## Report Block
Logs its decisions to `.claude/output/{feature-id}/_run-log.md` and,
where it affects cross-run state, to `run-history/_online-learning.yaml`
or `.claude/output/{feature-id}/_token-telemetry.json` as applicable.
