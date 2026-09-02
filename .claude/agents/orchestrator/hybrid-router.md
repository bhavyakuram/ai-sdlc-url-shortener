---
agent_id: hybrid-router
category: ORCH
phase: orchestrator
model_tier: haiku
conditional: hybrid+agentic modes
calls_skill: skills/orchestrator/hybrid-router/SKILL.md
---

# Agent: Hybrid Router

## Purpose
Adjusts downstream phase weighting and test-depth based on the risk profile surfaced in STEP-1

## Phase Placement
orchestrator

## Input Contract
risk-register.md

## Output Contract
routing directive consumed by planner/coverage-strategy

## Requires (capabilities)
none

## Conditional Firing
hybrid+agentic modes

## Contract
This agent is a declaration only: WHAT it does, WHEN it fires, WHAT it
requires, WHAT it produces. Implementation logic lives entirely in
`skills/orchestrator/hybrid-router/SKILL.md` per the framework's agent/skill
separation (Section 5, Key Design Principle #2).
