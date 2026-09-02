---
skill_id: state-migration
implements_agent: agents/step3/state-migration.md
model_tier: sonnet
---

# Skill: State Migration

## Implementation Logic
Plans database/state changes needed for the feature (schema diffs, backfill strategy)

Reads: technical-design.md, data-context (if db-harness configured)
Writes: step3/state-migration.md

> TODO(scaffold): flesh out the concrete step-by-step algorithm this
> skill executes (tool calls, decision logic, edge-case handling) once
> STEP-3/STEP-4 detail for the URL-shortener feature set is finalized.

## Tool Usage
db-harness (optional)

## Recipe Calls
Stack-scoped where applicable — resolved via `stacks/{active-stack}/stack-skills.yaml`
recipe lookup (see rules/architecture.md, Section 13.1 Technology Agnosticism).
No stack name, file extension, or grep pattern is hardcoded here.

## Report Block
On completion, appends a START/END entry to `.claude/output/{feature-id}/_run-log.md`
and writes the output(s) listed above. Every factual claim in the report
must be backed by a verbatim tool-call output (rules/architecture.md,
Proof Over Promise).
