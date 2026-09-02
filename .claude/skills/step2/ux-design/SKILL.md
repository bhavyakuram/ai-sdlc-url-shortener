---
skill_id: ux-design
implements_agent: agents/step2/ux-design.md
model_tier: sonnet
---

# Skill: UX Design

## Implementation Logic
Defines user flows, screen/response transitions, interaction patterns (reads Figma exports if present)

Reads: feature-spec.md, inputs/{feature-id}/figma/
Writes: step2/ux-flow.md

> TODO(scaffold): flesh out the concrete step-by-step algorithm this
> skill executes (tool calls, decision logic, edge-case handling) once
> STEP-3/STEP-4 detail for the URL-shortener feature set is finalized.

## Tool Usage
mcp:figma (optional)

## Recipe Calls
Stack-scoped where applicable — resolved via `stacks/{active-stack}/stack-skills.yaml`
recipe lookup (see rules/architecture.md, Section 13.1 Technology Agnosticism).
No stack name, file extension, or grep pattern is hardcoded here.

## Report Block
On completion, appends a START/END entry to `.claude/output/{feature-id}/_run-log.md`
and writes the output(s) listed above. Every factual claim in the report
must be backed by a verbatim tool-call output (rules/architecture.md,
Proof Over Promise).
