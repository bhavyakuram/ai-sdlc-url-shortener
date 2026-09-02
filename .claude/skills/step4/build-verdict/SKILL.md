---
skill_id: build-verdict
implements_agent: agents/step4/build-verdict.md
model_tier: haiku
---

# Skill: Build Verdict (Step 4.1)

## Implementation Logic
Reads compiler/language-server diagnostics to verify the generated code compiles; classifies findings into BLOCKER/HIGH/MEDIUM/LOW severity bands

Reads: Generated source tree
Writes: step4/build-verdict.md

> TODO(scaffold): flesh out the concrete step-by-step algorithm this
> skill executes (tool calls, decision logic, edge-case handling) once
> STEP-3/STEP-4 detail for the URL-shortener feature set is finalized.

## Tool Usage
Bash (mvn/gradle or pytest --collect-only, mypy)

## Recipe Calls
Stack-scoped where applicable — resolved via `stacks/{active-stack}/stack-skills.yaml`
recipe lookup (see rules/architecture.md, Section 13.1 Technology Agnosticism).
No stack name, file extension, or grep pattern is hardcoded here.

## Report Block
On completion, appends a START/END entry to `.claude/output/{feature-id}/_run-log.md`
and writes the output(s) listed above. Every factual claim in the report
must be backed by a verbatim tool-call output (rules/architecture.md,
Proof Over Promise).
