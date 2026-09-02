---
agent_id: evaluator
category: STEP5
phase: step5
model_tier: haiku
conditional: false
calls_skill: skills/step5/evaluator/SKILL.md
---

# Agent: Evaluator

## Purpose
Runs the test suite, checks coverage, verifies AC satisfaction; FAIL routes back to STEP-4

## Phase Placement
step5

## Input Contract
step5/tests/, generated source code

## Output Contract
step5/test-report.md + step5/evaluation-verdict.md

## Requires (capabilities)
Bash (mvn test / pytest --cov)

## Conditional Firing
false

## Contract
This agent is a declaration only: WHAT it does, WHEN it fires, WHAT it
requires, WHAT it produces. Implementation logic lives entirely in
`skills/step5/evaluator/SKILL.md` per the framework's agent/skill
separation (Section 5, Key Design Principle #2).
