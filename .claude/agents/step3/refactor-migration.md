---
agent_id: refactor-migration
category: STEP3
phase: step3
model_tier: sonnet
conditional: refactor/enhancement scenarios only
calls_skill: skills/step3/refactor-migration/SKILL.md
---

# Agent: Refactor Migration

## Purpose
Produces a migration matrix for refactor/brownfield scenarios (R1-R10 compatibility checks)

## Phase Placement
step3

## Input Contract
technical-design.md, impact-analysis.md

## Output Contract
step3/refactor-migration.md

## Requires (capabilities)
none

## Conditional Firing
refactor/enhancement scenarios only

## Contract
This agent is a declaration only: WHAT it does, WHEN it fires, WHAT it
requires, WHAT it produces. Implementation logic lives entirely in
`skills/step3/refactor-migration/SKILL.md` per the framework's agent/skill
separation (Section 5, Key Design Principle #2).
