---
skill_id: parallel-explorer
implements_agent: none (framework-level skill, no per-phase agent declaration)
---

# Skill: Parallel Explorer

## Purpose
Agentic mode: spawns up to 3 parallel branches within a single phase for independent sub-problems (e.g. three candidate technical-design approaches)

## Implementation Logic
Dispatches N parallel agent invocations with the same input contract, collects outputs, and either merges (non-conflicting) or asks the operator to pick (Gate-extension) when candidates conflict.

## Report Block
Logs its decisions to `.claude/output/{feature-id}/_run-log.md` and,
where it affects cross-run state, to `run-history/_online-learning.yaml`
or `.claude/output/{feature-id}/_token-telemetry.json` as applicable.
