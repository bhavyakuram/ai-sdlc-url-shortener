---
agent_id: generator
category: STEP4
phase: step4
model_tier: sonnet/opus (complex)
conditional: false
calls_skill: skills/step4/generator/SKILL.md
---

# Agent: Generator

## Purpose
Writes production code following the sprint plan exactly; multi-wave dispatch for 5+ files

## Phase Placement
step4

## Input Contract
sprint-plan.md, active stack standards/reference

## Output Contract
step4/generator-summary.md + generated source code

## Requires (capabilities)
stack-scoped (java:spring-boot or python:fastapi)

## Conditional Firing
false

## Contract
This agent is a declaration only: WHAT it does, WHEN it fires, WHAT it
requires, WHAT it produces. Implementation logic lives entirely in
`skills/step4/generator/SKILL.md` per the framework's agent/skill
separation (Section 5, Key Design Principle #2).
