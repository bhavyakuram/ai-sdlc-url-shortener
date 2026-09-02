---
agent_id: ux-prototype
category: STEP0
phase: step0
model_tier: sonnet
conditional: greenfield only
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
greenfield only

## Contract
This agent is a declaration only: WHAT it does, WHEN it fires, WHAT it
requires, WHAT it produces. Implementation logic lives entirely in
`skills/step0/ux-prototype/SKILL.md` per the framework's agent/skill
separation (Section 5, Key Design Principle #2).
