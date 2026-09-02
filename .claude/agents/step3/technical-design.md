---
agent_id: technical-design
category: STEP3
phase: step3
model_tier: opus
conditional: false
calls_skill: skills/step3/technical-design/SKILL.md
---

# Agent: Technical Design

## Purpose
Component decomposition, technology choices, API design, primitive selection (the HOW contract)

## Phase Placement
step3

## Input Contract
feature-spec.md, acceptance-criteria.md, architecture-context.md

## Output Contract
step3/technical-design.md

## Requires (capabilities)
none

## Conditional Firing
false

## Contract
This agent is a declaration only: WHAT it does, WHEN it fires, WHAT it
requires, WHAT it produces. Implementation logic lives entirely in
`skills/step3/technical-design/SKILL.md` per the framework's agent/skill
separation (Section 5, Key Design Principle #2).
