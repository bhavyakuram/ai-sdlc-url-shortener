---
skill_id: online-learning
implements_agent: none (framework-level skill, no per-phase agent declaration)
---

# Skill: Online Learning

## Purpose
Auto-handles Gate 4 and Gate 5 after 10 consistent identical approvals for the same matrix row (pattern-based auto-approval)

## Implementation Logic
Reads run-history/_online-learning.yaml -> if the last 10 decisions for this gate+matrix-row combination were identical, auto-applies that decision and logs it as auto-approved (never for Gates 0-3, per rules/mode-policy.md bounded-autonomy limits).

## Report Block
Logs its decisions to `.claude/output/{feature-id}/_run-log.md` and,
where it affects cross-run state, to `run-history/_online-learning.yaml`
or `.claude/output/{feature-id}/_token-telemetry.json` as applicable.
