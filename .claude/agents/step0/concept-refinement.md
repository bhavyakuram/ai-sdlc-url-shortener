---
agent_id: concept-refinement
category: STEP0
phase: step0
model_tier: sonnet
conditional: feature_shape=greenfield-app only
calls_skill: skills/step0/concept-refinement/SKILL.md
---

# Agent: Concept Refinement

## Purpose
Transforms a freeform idea.md into a structured concept.md: named entities, personas, MVP features (MoSCoW), success metrics

## Phase Placement
step0

## Input Contract
inputs/{feature-id}/ideation/idea.md

## Output Contract
step0/concept.md

## Requires (capabilities)
none

## Conditional Firing
feature_shape=greenfield-app only

## Contract
This agent is a declaration only: WHAT it does, WHEN it fires, WHAT it
requires, WHAT it produces. Implementation logic lives entirely in
`skills/step0/concept-refinement/SKILL.md` per the framework's agent/skill
separation (Section 5, Key Design Principle #2).
