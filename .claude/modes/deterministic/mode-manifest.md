---
mode_id: deterministic
---

# Mode: deterministic

**CLAUDE.md routing only — no decision surfaces.** Simplest, most
predictable. Every gate is always active; no auto-approval; no
conductor, no parallel-explorer, no online-learning.

## HITL Gates
All 8 gates always active (see `reference/mode-catalog.md`).

## Decision Authority
None delegated. Every branch point in the pipeline is either a fixed
rule (`rules/`) or a human decision (a Gate). No agent makes a judgment
call that isn't fully specified by the rules layer.

## When To Use
First runs against a new stack, or any run where the operator wants
maximum predictability over speed — e.g. a live interview demo where
every step should be inspectable.
