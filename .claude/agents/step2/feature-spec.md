---
agent_id: feature-spec
category: STEP2
phase: step2
model_tier: sonnet
conditional: false
calls_skill: skills/step2/feature-spec/SKILL.md
---

# Agent: Feature Spec

## Purpose
Translates the PRD into a detailed functional feature specification (the WHAT contract)

## Phase Placement
step2

## Input Contract
prd-v0.md, feasibility-report.md, risk-register.md

## Output Contract
step2/feature-spec.md

## Requires (capabilities)
none

## Conditional Firing
false

## Contract
This agent is a declaration only: WHAT it does, WHEN it fires, WHAT it
requires, WHAT it produces. Implementation logic lives entirely in
`skills/step2/feature-spec/SKILL.md` per the framework's agent/skill
separation (Section 5, Key Design Principle #2).
