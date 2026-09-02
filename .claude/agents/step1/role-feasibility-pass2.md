---
agent_id: role-feasibility-pass2
category: STEP1
phase: step1
model_tier: haiku
conditional: mod postures only
calls_skill: skills/step1/role-feasibility-pass2/SKILL.md
---

# Agent: Role Feasibility (Pass 2)

## Purpose
*-mod only: refines role-fit determination using impact + risk data from Stage 1-3

## Phase Placement
step1

## Input Contract
impact-analysis.md, risk-register.md

## Output Contract
step1/role-feasibility-pass2.md

## Requires (capabilities)
none

## Conditional Firing
mod postures only

## Contract
This agent is a declaration only: WHAT it does, WHEN it fires, WHAT it
requires, WHAT it produces. Implementation logic lives entirely in
`skills/step1/role-feasibility-pass2/SKILL.md` per the framework's agent/skill
separation (Section 5, Key Design Principle #2).
