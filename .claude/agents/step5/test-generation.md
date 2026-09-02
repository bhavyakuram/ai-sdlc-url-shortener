---
agent_id: test-generation
category: STEP5
phase: step5
model_tier: sonnet
conditional: false
calls_skill: skills/step5/test-generation/SKILL.md
---

# Agent: Test Generation

## Purpose
Writes automated tests for every acceptance criterion

## Phase Placement
step5

## Input Contract
acceptance-criteria.md, generated source code

## Output Contract
step5/tests/ (generated test files)

## Requires (capabilities)
stack-scoped test framework (JUnit or pytest)

## Conditional Firing
false

## Contract
This agent is a declaration only: WHAT it does, WHEN it fires, WHAT it
requires, WHAT it produces. Implementation logic lives entirely in
`skills/step5/test-generation/SKILL.md` per the framework's agent/skill
separation (Section 5, Key Design Principle #2).
