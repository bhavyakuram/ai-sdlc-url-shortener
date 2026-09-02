---
skill_id: security-audit
implements_agent: agents/step6/security-audit.md
model_tier: sonnet
---

# Skill: Security Audit

## Implementation Logic
Zero-regression security posture check: secrets, injection risk, authz gaps, dependency CVEs

Reads: Generated source tree, dependency-audit.md
Writes: step6/security-audit-report.md

> TODO(scaffold): flesh out the concrete step-by-step algorithm this
> skill executes (tool calls, decision logic, edge-case handling) once
> STEP-3/STEP-4 detail for the URL-shortener feature set is finalized.

## Tool Usage
Bash (dependency-check or pip-audit/bandit)

## Recipe Calls
Stack-scoped where applicable — resolved via `stacks/{active-stack}/stack-skills.yaml`
recipe lookup (see rules/architecture.md, Section 13.1 Technology Agnosticism).
No stack name, file extension, or grep pattern is hardcoded here.

## Report Block
On completion, appends a START/END entry to `.claude/output/{feature-id}/_run-log.md`
and writes the output(s) listed above. Every factual claim in the report
must be backed by a verbatim tool-call output (rules/architecture.md,
Proof Over Promise).
