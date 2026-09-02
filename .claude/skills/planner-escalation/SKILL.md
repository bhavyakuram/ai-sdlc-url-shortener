---
skill_id: planner-escalation
implements_agent: none (framework-level skill, no per-phase agent declaration)
---

# Skill: Planner Escalation

## Purpose
On a second retry into STEP-4, decides whether to split the remaining scope into smaller waves or simplify the approach

## Implementation Logic
Reads retry count + build-verdict/evaluator failure reasons -> if failure is complexity-driven, re-invokes planner with a directive to split into smaller waves; if approach-driven, re-invokes technical-design with the failure reason attached.

## Report Block
Logs its decisions to `.claude/output/{feature-id}/_run-log.md` and,
where it affects cross-run state, to `run-history/_online-learning.yaml`
or `.claude/output/{feature-id}/_token-telemetry.json` as applicable.
