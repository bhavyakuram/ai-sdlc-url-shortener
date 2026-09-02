---
agent_id: acceptance-criteria
category: STEP2
phase: step2
model_tier: sonnet
conditional: false
calls_skill: skills/step2/acceptance-criteria/SKILL.md
---

# Agent: Acceptance Criteria

## Purpose
Writes GIVEN/WHEN/THEN acceptance criteria for every behavior in the spec

## Phase Placement
step2

## Input Contract
feature-spec.md, ux-flow.md

## Output Contract
step2/acceptance-criteria.md

## Requires (capabilities)
none

## Conditional Firing
false

## Contract
This agent is a declaration only: WHAT it does, WHEN it fires, WHAT it
requires, WHAT it produces. Implementation logic lives entirely in
`skills/step2/acceptance-criteria/SKILL.md` per the framework's agent/skill
separation (Section 5, Key Design Principle #2).
