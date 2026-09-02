---
skill_id: coverage-strategy
implements_agent: none (framework-level skill, no per-phase agent declaration)
---

# Skill: Coverage Strategy

## Purpose
Recommends a test coverage tier (smoke / full / risk-weighted) based on the STEP-1 risk profile

## Implementation Logic
Reads risk-register.md risk_level -> low risk: smoke tier; medium: full tier; high: risk-weighted tier (extra depth on the specific risk areas flagged) -> passed to test-generation/coverage-edge as a directive.

## Report Block
Logs its decisions to `.claude/output/{feature-id}/_run-log.md` and,
where it affects cross-run state, to `run-history/_online-learning.yaml`
or `.claude/output/{feature-id}/_token-telemetry.json` as applicable.
