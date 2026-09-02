---
agent_id: pattern-extractor
category: ORCH
phase: orchestrator
model_tier: haiku
conditional: agentic mode only
calls_skill: skills/orchestrator/pattern-extractor/SKILL.md
---

# Agent: Pattern Extractor

## Purpose
Extracts recurring approval/rejection patterns from run-history for the online-learning skill

## Phase Placement
orchestrator

## Input Contract
run-history/_online-learning.yaml

## Output Contract
updated online-learning.yaml

## Requires (capabilities)
none

## Conditional Firing
agentic mode only

## Contract
This agent is a declaration only: WHAT it does, WHEN it fires, WHAT it
requires, WHAT it produces. Implementation logic lives entirely in
`skills/orchestrator/pattern-extractor/SKILL.md` per the framework's agent/skill
separation (Section 5, Key Design Principle #2).
