---
agent_id: ux-prototype
category: STEP0
phase: step0
model_tier: sonnet
conditional: greenfield AND active stack declares a `frontend` layer
calls_skill: skills/step0/ux-prototype/SKILL.md
---

# Agent: UX Prototype

## Purpose
Generates a self-contained, navigable static HTML prototype (max 12 screens, zero external deps)

## Phase Placement
step0

## Input Contract
step0/concept.md (Gate 0 approved)

## Output Contract
step0/ux-prototype/ (HTML files)

## Requires (capabilities)
none

## Conditional Firing
Fires only when BOTH hold: `role=greenfield` AND the active stack's
`stack-manifest.md` declares a `frontend` layer. An API/service-only
stack (e.g. `java-spring`, `python-fastapi` as declared for this
project) has no UI to prototype — firing anyway would produce an
illustrative mockup disconnected from what `generator` actually
builds, which is a cost with no corresponding benefit. This was
initially specified as "greenfield only" (unconditional); corrected
after a run demonstrated the mismatch in practice.

## Contract
This agent is a declaration only: WHAT it does, WHEN it fires, WHAT it
requires, WHAT it produces. Implementation logic lives entirely in
`skills/step0/ux-prototype/SKILL.md` per the framework's agent/skill
separation (Section 5, Key Design Principle #2).
