---
skill_id: test-generation
implements_agent: agents/step5/test-generation.md
model_tier: sonnet
---

# Skill: Test Generation

## Implementation Logic
Writes automated tests for every acceptance criterion

Reads: acceptance-criteria.md, generated source code
Writes: step5/tests/ (generated test files)

> TODO(scaffold): flesh out the concrete step-by-step algorithm this
> skill executes (tool calls, decision logic, edge-case handling) once
> STEP-3/STEP-4 detail for the URL-shortener feature set is finalized.

## Tool Usage
stack-scoped test framework (JUnit or pytest)

## Recipe Calls
Stack-scoped where applicable — resolved via `stacks/{active-stack}/stack-skills.yaml`
recipe lookup (see rules/architecture.md, Section 13.1 Technology Agnosticism).
No stack name, file extension, or grep pattern is hardcoded here.

## Report Block
On completion, appends a START/END entry to `.claude/output/{feature-id}/_run-log.md`
and writes the output(s) listed above. Every factual claim in the report
must be backed by a verbatim tool-call output (rules/architecture.md,
Proof Over Promise).
