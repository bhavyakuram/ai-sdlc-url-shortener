---
skill_id: orchestrator
implements_agent: none (framework-level skill, no per-phase agent declaration)
---

# Skill: Orchestrator (CI Hook)

## Purpose
Framework-agnostic CI entry point: cost-anomaly detection, Jira write-back, structured review-comment emission

## Implementation Logic
Exposes `sdlc-cli run` for CI invocation -> on completion, writes back the grading verdict to the originating Jira ticket (mcp:jira) and emits _ci-review-comments.json for PR-inline posting -> flags cost anomalies (run cost > 3x the stack's historical median) to the operator.

## Report Block
Logs its decisions to `.claude/output/{feature-id}/_run-log.md` and,
where it affects cross-run state, to `run-history/_online-learning.yaml`
or `.claude/output/{feature-id}/_token-telemetry.json` as applicable.
