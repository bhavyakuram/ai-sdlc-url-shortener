---
skill_id: role-resolver
implements_agent: none (framework-level skill, no per-phase agent declaration)
---

# Skill: Role Resolver

## Purpose
Resolves the active role, merges quality_policies from the stack manifest, resolves gate thresholds, writes _role-context.yaml

## Implementation Logic
Loads roles/{role}/role-manifest.md -> resolves quality_policies against stack declarations -> resolves per-gate thresholds -> writes .claude/output/{feature-id}/_role-context.yaml (the single source every downstream agent reads for role/policy/mode/threshold state).

## Report Block
Logs its decisions to `.claude/output/{feature-id}/_run-log.md` and,
where it affects cross-run state, to `run-history/_online-learning.yaml`
or `.claude/output/{feature-id}/_token-telemetry.json` as applicable.
