---
skill_id: generator
implements_agent: agents/step4/generator.md
model_tier: sonnet/opus (complex)
---

# Skill: Generator

## Implementation Logic
Writes production code following the sprint plan exactly; multi-wave dispatch for 5+ files

Reads: sprint-plan.md, active stack standards/reference
Writes: step4/generator-summary.md + generated source code

> TODO(scaffold): flesh out the concrete step-by-step algorithm this
> skill executes (tool calls, decision logic, edge-case handling) once
> STEP-3/STEP-4 detail for the URL-shortener feature set is finalized.

## Tool Usage
stack-scoped (java:spring-boot or python:fastapi)

## Recipe Calls
Stack-scoped where applicable — resolved via `stacks/{active-stack}/stack-skills.yaml`
recipe lookup (see rules/architecture.md, Section 13.1 Technology Agnosticism).
No stack name, file extension, or grep pattern is hardcoded here.

## Report Block
On completion, appends a START/END entry to `.claude/output/{feature-id}/_run-log.md`
and writes the output(s) listed above. Every factual claim in the report
must be backed by a verbatim tool-call output (rules/architecture.md,
Proof Over Promise).
