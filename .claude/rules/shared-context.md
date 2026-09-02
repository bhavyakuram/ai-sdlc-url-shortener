# Rule: Shared Context

**Category:** Process · **Priority:** 3

## Content-Addressed Caching
`architecture-context.md` and `codebase-context.md` are expensive to
produce (~55k tokens cold). They are cached at
`shared-context/{stack}/snapshots/{sha}/`, keyed by a content hash of
the repo state (computed from tracked file hashes, not just a git SHA,
so uncommitted changes still invalidate correctly).

## Cache Behavior
| Scenario | Result |
|---|---|
| First run on a stack | cold bootstrap |
| Re-run, same stack, same code | cache hit |
| Different feature, same stack, same code | cache hit |
| Different stack | cold for the new stack; old cache untouched |
| Code changed since last snapshot | cache miss, regenerate |

## Digest-First Reading
Every agent reads the ~2KB `*-digest.md` summary first and only pulls
specific sections of the full `*-context.md` file when the digest
indicates relevance. This is not optional — it is the primary token-cost
control for PRE-WORK and STEP-1 through STEP-6.
