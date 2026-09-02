---
agent_id: feasibility
category: STEP1
phase: step1
model_tier: sonnet
conditional: false
calls_skill: skills/step1/feasibility/SKILL.md
---

# Agent: Feasibility

## Purpose
Determines whether the feature can be built as specified; surfaces blocking constraints

## Phase Placement
step1

## Input Contract
prework/prd-v0.md, architecture-context.md

## Output Contract
step1/feasibility-report.md

## Requires (capabilities)
none

## Conditional Firing
false

## Contract
This agent is a declaration only: WHAT it does, WHEN it fires, WHAT it
requires, WHAT it produces. Implementation logic lives entirely in
`skills/step1/feasibility/SKILL.md` per the framework's agent/skill
separation (Section 5, Key Design Principle #2).
