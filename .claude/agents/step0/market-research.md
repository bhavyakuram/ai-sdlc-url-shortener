---
agent_id: market-research
category: STEP0
phase: step0
model_tier: sonnet
conditional: greenfield only
calls_skill: skills/step0/market-research/SKILL.md
---

# Agent: Market Research

## Purpose
Validates the concept via technology-landscape analysis, similar-product comparison, stack-fit assessment

## Phase Placement
step0

## Input Contract
step0/concept.md

## Output Contract
step0/market-research.md

## Requires (capabilities)
WebSearch

## Conditional Firing
greenfield only

## Contract
This agent is a declaration only: WHAT it does, WHEN it fires, WHAT it
requires, WHAT it produces. Implementation logic lives entirely in
`skills/step0/market-research/SKILL.md` per the framework's agent/skill
separation (Section 5, Key Design Principle #2).
