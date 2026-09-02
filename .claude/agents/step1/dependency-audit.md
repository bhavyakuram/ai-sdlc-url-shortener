---
agent_id: dependency-audit
category: STEP1
phase: step1
model_tier: haiku
conditional: false
calls_skill: skills/step1/dependency-audit/SKILL.md
---

# Agent: Dependency Audit

## Purpose
Identifies external dependencies (libraries, services, DB engines) affected by or required for the feature

## Phase Placement
step1

## Input Contract
prework/prd-v0.md, stack-manifest.md

## Output Contract
step1/dependency-audit.md

## Requires (capabilities)
none

## Conditional Firing
false

## Contract
This agent is a declaration only: WHAT it does, WHEN it fires, WHAT it
requires, WHAT it produces. Implementation logic lives entirely in
`skills/step1/dependency-audit/SKILL.md` per the framework's agent/skill
separation (Section 5, Key Design Principle #2).
