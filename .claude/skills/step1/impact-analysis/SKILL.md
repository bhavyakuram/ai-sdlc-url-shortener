---
skill_id: impact-analysis
implements_agent: agents/step1/impact-analysis.md
model_tier: sonnet
---

# Skill: Impact Analysis

## Implementation Logic
Determines what existing code, contracts, and data will be affected (brownfield-critical)

Reads: codebase-context.md, feasibility-report.md
Writes: step1/impact-analysis.md

> TODO(scaffold): flesh out the concrete step-by-step algorithm this
> skill executes (tool calls, decision logic, edge-case handling) once
> STEP-3/STEP-4 detail for the URL-shortener feature set is finalized.

## Tool Usage
code-graph tier3 (find_callers/callees)

## Recipe Calls
Stack-scoped where applicable — resolved via `stacks/{active-stack}/stack-skills.yaml`
recipe lookup (see rules/architecture.md, Section 13.1 Technology Agnosticism).
No stack name, file extension, or grep pattern is hardcoded here.

## Report Block
On completion, appends a START/END entry to `.claude/output/{feature-id}/_run-log.md`
and writes the output(s) listed above. Every factual claim in the report
must be backed by a verbatim tool-call output (rules/architecture.md,
Proof Over Promise).
