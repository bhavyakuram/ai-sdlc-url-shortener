---
skill_id: git-history-capture
implements_agent: agents/step1/git-history-capture.md
model_tier: haiku
---

# Skill: Git History Capture

## Implementation Logic
*-mod + incident-fix only: analyzes git history for hotspots, co-change clusters, regression markers

Reads: git log of in-scope files
Writes: step1/git-history.md

> TODO(scaffold): flesh out the concrete step-by-step algorithm this
> skill executes (tool calls, decision logic, edge-case handling) once
> STEP-3/STEP-4 detail for the URL-shortener feature set is finalized.

## Tool Usage
Bash(git log/blame)

## Recipe Calls
Stack-scoped where applicable — resolved via `stacks/{active-stack}/stack-skills.yaml`
recipe lookup (see rules/architecture.md, Section 13.1 Technology Agnosticism).
No stack name, file extension, or grep pattern is hardcoded here.

## Report Block
On completion, appends a START/END entry to `.claude/output/{feature-id}/_run-log.md`
and writes the output(s) listed above. Every factual claim in the report
must be backed by a verbatim tool-call output (rules/architecture.md,
Proof Over Promise).
