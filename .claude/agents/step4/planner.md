---
agent_id: planner
category: STEP4
phase: step4
model_tier: sonnet
conditional: false
calls_skill: skills/step4/planner/SKILL.md
---

# Agent: Planner

## Purpose
Decomposes the technical design into an ordered sprint plan: file create/modify lists, dispatch waves

## Phase Placement
step4

## Input Contract
technical-design.md, api-contract.yaml, state-migration.md

## Output Contract
step4/sprint-plan.md

## Requires (capabilities)
none

## Conditional Firing
false

## Contract
This agent is a declaration only: WHAT it does, WHEN it fires, WHAT it
requires, WHAT it produces. Implementation logic lives entirely in
`skills/step4/planner/SKILL.md` per the framework's agent/skill
separation (Section 5, Key Design Principle #2).
