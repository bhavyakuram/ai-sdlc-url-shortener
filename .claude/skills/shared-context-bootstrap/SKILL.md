---
skill_id: shared-context-bootstrap
implements_agent: none (framework-level skill, no per-phase agent declaration)
---

# Skill: Shared Context Bootstrap

## Purpose
Computes or reuses the content-addressed architecture/codebase context snapshot for the active stack

## Implementation Logic
Computes a content hash of the repo state -> checks shared-context/{stack}/snapshots/{sha}/ for a cache hit -> on miss, dispatches architecture-analysis + codebase-context fresh and writes a new snapshot -> on hit, reuses (saving ~30k+ tokens per run).

## Report Block
Logs its decisions to `.claude/output/{feature-id}/_run-log.md` and,
where it affects cross-run state, to `run-history/_online-learning.yaml`
or `.claude/output/{feature-id}/_token-telemetry.json` as applicable.
