---
agent_id: conductor
category: ORCH
phase: orchestrator
model_tier: sonnet
conditional: agentic mode only
calls_skill: skills/orchestrator/conductor/SKILL.md
---

# Agent: Conductor

## Purpose
Agentic-mode only: sequences STEP-N to STEP-N+1 without full HITL at every boundary, within bounded autonomy limits

## Phase Placement
orchestrator

## Input Contract
_role-context.yaml, transition-fsm state

## Output Contract
_run-log.md entries

## Requires (capabilities)
none

## Conditional Firing
agentic mode only

## Contract
This agent is a declaration only: WHAT it does, WHEN it fires, WHAT it
requires, WHAT it produces. Implementation logic lives entirely in
`skills/orchestrator/conductor/SKILL.md` per the framework's agent/skill
separation (Section 5, Key Design Principle #2).
