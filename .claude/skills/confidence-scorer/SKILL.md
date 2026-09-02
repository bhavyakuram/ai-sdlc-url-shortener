---
skill_id: confidence-scorer
implements_agent: none (framework-level skill, no per-phase agent declaration)
---

# Skill: Confidence Scorer

## Purpose
Emits a high/medium/low confidence signal on every gate output for the operator's benefit

## Implementation Logic
Scores agent-reported certainty + evidence density (tool-call count backing the claim, per rules/proof-over-promise) -> attaches confidence to every Gate prompt so the operator knows how much scrutiny a decision needs.

## Report Block
Logs its decisions to `.claude/output/{feature-id}/_run-log.md` and,
where it affects cross-run state, to `run-history/_online-learning.yaml`
or `.claude/output/{feature-id}/_token-telemetry.json` as applicable.
