---
skill_id: shared-context-bootstrap
implements_agent: none (framework-level skill, no per-phase agent declaration)
---

# Skill: Shared Context Bootstrap

## Purpose
Computes or reuses the content-addressed architecture/codebase context snapshot for the active stack

## Implementation Logic
1. Compute a content hash of the repo state: `find <stack-source-root> -type f | sort | xargs cat | md5sum`, truncated to 12 hex chars. For a greenfield run with no source tree yet, use the literal key `greenfield-baseline` instead of a hash.
2. Check `shared-context/{stack}/snapshots/{key}/` for `architecture-context.md` **and** `codebase-context.md` both present — that pair, not just the folder, is what defines a cache hit. A folder containing only `manifest.json` (e.g. because a run only recorded *that* a miss happened, without writing the analysis back) is **not** a hit — treat it as a miss and backfill it.
3. **On a hit**: reuse both files and their digests; do not re-dispatch `architecture-analysis`/`codebase-context`. Log the reuse (and the ~30k+ token saving) in `_run-log.md`.
4. **On a miss**: dispatch `architecture-analysis` and `codebase-context` for real, then write ALL of the following before considering the snapshot complete:
   - `architecture-context.md` — the full analysis (layers, dependencies, patterns, or clean-slate declaration for greenfield)
   - `architecture-digest.md` — a ~2KB summary of the above, for digest-first reading by every later agent
   - `codebase-context.md` — in-scope files, layer map, drift-vs-standards (or N/A for greenfield)
   - `codebase-digest.md` — ~2KB summary
   - `manifest.json` — `snapshot_sha`, `stack`, `cache_status`, and (on a miss) `prior_snapshot` + `reason`
5. Data-context ingestion if `db-harness/` is configured (not applicable to this project — no `db-harness/` configured).

## Lifecycle Note (added after a real gap was found and fixed)
A snapshot taken at PRE-WORK time reflects the codebase *before* that
same run's own STEP-4 changes land. If `generator` changes the
codebase during the run, the snapshot computed at entry is now stale
for any *later* run — this is correct, expected cache-miss behavior,
not a bug. To keep the cache actually useful, `shared-context-bootstrap`
should be re-invoked (or the snapshot refreshed) once more after a
run completes with code changes, so the *next* run's PRE-WORK entry
can genuinely hit cache if nothing changed in between.

## Report Block
Logs its decisions to `.claude/output/{feature-id}/_run-log.md` and,
where it affects cross-run state, to `run-history/_online-learning.yaml`
or `.claude/output/{feature-id}/_token-telemetry.json` as applicable.
