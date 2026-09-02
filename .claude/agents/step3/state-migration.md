---
agent_id: state-migration
category: STEP3
phase: step3
model_tier: sonnet
conditional: false
calls_skill: skills/step3/state-migration/SKILL.md
---

# Agent: State Migration

## Purpose
Plans database/state changes needed for the feature (schema diffs, backfill strategy)

## Phase Placement
step3

## Input Contract
technical-design.md, data-context (if db-harness configured)

## Output Contract
step3/state-migration.md

## Requires (capabilities)
db-harness (optional)

## Conditional Firing
false

## Contract
This agent is a declaration only: WHAT it does, WHEN it fires, WHAT it
requires, WHAT it produces. Implementation logic lives entirely in
`skills/step3/state-migration/SKILL.md` per the framework's agent/skill
separation (Section 5, Key Design Principle #2).
