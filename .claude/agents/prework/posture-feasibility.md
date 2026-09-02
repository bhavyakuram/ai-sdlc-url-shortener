---
agent_id: posture-feasibility
category: PREWORK
phase: prework
model_tier: haiku
conditional: false
calls_skill: skills/prework/posture-feasibility/SKILL.md
---

# Agent: Posture Feasibility

## Purpose
Always-on check: does the codebase evidence match the filed posture (dev/mod/doc/greenfield)?

## Phase Placement
prework

## Input Contract
triage-verdict.md, codebase-context.md

## Output Contract
prework/posture-feasibility-verdict.md

## Requires (capabilities)
none

## Conditional Firing
false

## Contract
This agent is a declaration only: WHAT it does, WHEN it fires, WHAT it
requires, WHAT it produces. Implementation logic lives entirely in
`skills/prework/posture-feasibility/SKILL.md` per the framework's agent/skill
separation (Section 5, Key Design Principle #2).
