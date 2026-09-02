---
skill_id: role-feasibility-pass2
implements_agent: agents/step1/role-feasibility-pass2.md
model_tier: haiku
---

# Skill: Role Feasibility (Pass 2)

## Implementation Logic
*-mod only: refines role-fit determination using impact + risk data from Stage 1-3

Reads: impact-analysis.md, risk-register.md
Writes: step1/role-feasibility-pass2.md

> TODO(scaffold): flesh out the concrete step-by-step algorithm this
> skill executes (tool calls, decision logic, edge-case handling) once
> STEP-3/STEP-4 detail for the URL-shortener feature set is finalized.

## Tool Usage
none

## Recipe Calls
Stack-scoped where applicable — resolved via `stacks/{active-stack}/stack-skills.yaml`
recipe lookup (see rules/architecture.md, Section 13.1 Technology Agnosticism).
No stack name, file extension, or grep pattern is hardcoded here.

## Report Block
On completion, appends a START/END entry to `.claude/output/{feature-id}/_run-log.md`
and writes the output(s) listed above. Every factual claim in the report
must be backed by a verbatim tool-call output (rules/architecture.md,
Proof Over Promise).
