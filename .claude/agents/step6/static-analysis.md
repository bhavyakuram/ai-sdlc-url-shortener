---
agent_id: static-analysis
category: STEP6
phase: step6
model_tier: haiku
conditional: false
calls_skill: skills/step6/static-analysis/SKILL.md
---

# Agent: Static Analysis

## Purpose
Runs lint/static analysis on in-scope files; reports code-quality findings

## Phase Placement
step6

## Input Contract
Generated source tree

## Output Contract
step6/static-analysis-report.md

## Requires (capabilities)
Bash (checkstyle/spotbugs or ruff/mypy)

## Conditional Firing
false

## Contract
This agent is a declaration only: WHAT it does, WHEN it fires, WHAT it
requires, WHAT it produces. Implementation logic lives entirely in
`skills/step6/static-analysis/SKILL.md` per the framework's agent/skill
separation (Section 5, Key Design Principle #2).
