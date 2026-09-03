---
skill_id: ux-prototype
implements_agent: agents/step0/ux-prototype.md
model_tier: sonnet
---

# Skill: UX Prototype

## Implementation Logic
**Step 0 — applicability check** (runs before anything else): read
the active stack's `stack-manifest.md` Layers declaration. If it does
not list a `frontend` layer, this skill does not fire — write a single
`step0/ux-prototype-skipped.md` note (agent id, reason, stack checked)
and return immediately. `java-spring` and `python-fastapi`, as
declared for this project, both fail this check.

If a `frontend` layer IS declared: generate a self-contained,
navigable static HTML prototype (max 12 screens, zero external deps).

Reads: step0/concept.md (Gate 0 approved), active stack-manifest.md
Writes: step0/ux-prototype/ (HTML files) — or step0/ux-prototype-skipped.md if the applicability check fails

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
