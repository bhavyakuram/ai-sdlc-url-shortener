---
skill_id: architecture-analysis
implements_agent: agents/prework/architecture-analysis.md
model_tier: sonnet
---

# Skill: Architecture Analysis

## Implementation Logic
Analyzes existing codebase architecture: layers, dependencies, patterns (brownfield) or declares clean-slate baseline (greenfield)

Reads: Repo source tree
Writes: shared-context/{stack}/snapshots/{sha}/architecture-context.md + architecture-digest.md

> TODO(scaffold): flesh out the concrete step-by-step algorithm this
> skill executes (tool calls, decision logic, edge-case handling) once
> STEP-3/STEP-4 detail for the URL-shortener feature set is finalized.

## Tool Usage
code-graph tier2/3

## Recipe Calls
Stack-scoped where applicable — resolved via `stacks/{active-stack}/stack-skills.yaml`
recipe lookup (see rules/architecture.md, Section 13.1 Technology Agnosticism).
No stack name, file extension, or grep pattern is hardcoded here.

## Report Block
On completion, appends a START/END entry to `.claude/output/{feature-id}/_run-log.md`
and writes the output(s) listed above. Every factual claim in the report
must be backed by a verbatim tool-call output (rules/architecture.md,
Proof Over Promise).
