---
skill_id: platform-resolver
implements_agent: none (framework-level skill, no per-phase agent declaration)
---

# Skill: Platform Resolver

## Purpose
Resolves the platform (cloud) overlay, if any, and injects platform-specific standards at rule-priority 4.25

## Implementation Logic
Reads platforms/{platform}/standards/ (default: platforms/none, i.e. no overlay) -> merges into the cached shared-context block below stack standards but above role manifest in the priority hierarchy.

## Report Block
Logs its decisions to `.claude/output/{feature-id}/_run-log.md` and,
where it affects cross-run state, to `run-history/_online-learning.yaml`
or `.claude/output/{feature-id}/_token-telemetry.json` as applicable.
