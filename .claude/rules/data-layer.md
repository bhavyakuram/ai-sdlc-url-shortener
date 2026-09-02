# Rule: Data Layer

**Category:** Data · **Priority:** 3

## Pluggable Database Engine
The URL shortener's persistence choice (in-memory for the prototype
default, or a real engine via `db-harness/`) is a stack-manifest
declaration (`data:relational` / `data:kv` capability tokens), never
hardcoded in an agent or skill.

## Bottom-Up Schema Integration
When `db-harness/` is configured, `state-migration` (STEP-3) reads the
actual schema from `shared-context/{stack}/data-context/{baseline-sha}/`
rather than inferring it from code — schema is ground truth, code
follows it.

## Migration Safety
Any schema change proposed by `state-migration` must be additive-first
(new nullable columns, new tables) unless the feature explicitly
requires a breaking change, in which case Gate 3 (architect sign-off)
must see the migration plan before STEP-4 generates code against it.
