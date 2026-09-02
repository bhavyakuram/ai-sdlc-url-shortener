---
agent_id: coverage-edge
category: STEP5
phase: step5
model_tier: sonnet
conditional: false
calls_skill: skills/step5/coverage-edge/SKILL.md
---

# Agent: Coverage & Edge Cases

## Purpose
Identifies edge cases and negative test scenarios beyond the literal ACs

## Phase Placement
step5

## Input Contract
acceptance-criteria.md, api-contract.yaml

## Output Contract
step5/coverage-edge.md + additional tests

## Requires (capabilities)
none

## Conditional Firing
false

## Contract
This agent is a declaration only: WHAT it does, WHEN it fires, WHAT it
requires, WHAT it produces. Implementation logic lives entirely in
`skills/step5/coverage-edge/SKILL.md` per the framework's agent/skill
separation (Section 5, Key Design Principle #2).
