---
agent_id: role-feasibility-pass1
category: PREWORK
phase: prework
model_tier: haiku
conditional: mod postures only
calls_skill: skills/prework/role-feasibility-pass1/SKILL.md
---

# Agent: Role Feasibility (Pass 1)

## Purpose
*-mod posture only: does discovered code surface match the active role's declared layers?

## Phase Placement
prework

## Input Contract
codebase-context.md, active role-manifest.md

## Output Contract
prework/role-feasibility-pass1.md

## Requires (capabilities)
none

## Conditional Firing
mod postures only

## Contract
This agent is a declaration only: WHAT it does, WHEN it fires, WHAT it
requires, WHAT it produces. Implementation logic lives entirely in
`skills/prework/role-feasibility-pass1/SKILL.md` per the framework's agent/skill
separation (Section 5, Key Design Principle #2).
