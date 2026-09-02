---
skill_id: adaptive-gate
implements_agent: none (framework-level skill, no per-phase agent declaration)
---

# Skill: Adaptive Gate

## Purpose
Calibrates the per-role-lane retry budget (3-8, default 5) based on historical pass rates for this stack+role combination

## Implementation Logic
Reads run-history/_online-learning.yaml pass/fail history for the matrix row -> raises the budget toward 8 for consistently-passing lanes, lowers toward 3 for consistently-failing ones; never exceeds the hard ceiling in rules/retry-policy.md.

## Report Block
Logs its decisions to `.claude/output/{feature-id}/_run-log.md` and,
where it affects cross-run state, to `run-history/_online-learning.yaml`
or `.claude/output/{feature-id}/_token-telemetry.json` as applicable.
