---
agent_id: api-contract
category: STEP3
phase: step3
model_tier: sonnet
conditional: false
calls_skill: skills/step3/api-contract/SKILL.md
---

# Agent: API Contract

## Purpose
Defines the API schema (OpenAPI 3.1 for the REST endpoints)

## Phase Placement
step3

## Input Contract
technical-design.md

## Output Contract
step3/api-contract.yaml

## Requires (capabilities)
none

## Conditional Firing
false

## Contract
This agent is a declaration only: WHAT it does, WHEN it fires, WHAT it
requires, WHAT it produces. Implementation logic lives entirely in
`skills/step3/api-contract/SKILL.md` per the framework's agent/skill
separation (Section 5, Key Design Principle #2).
