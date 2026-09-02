---
agent_id: impact-analysis
category: STEP1
phase: step1
model_tier: sonnet
conditional: false
calls_skill: skills/step1/impact-analysis/SKILL.md
---

# Agent: Impact Analysis

## Purpose
Determines what existing code, contracts, and data will be affected (brownfield-critical)

## Phase Placement
step1

## Input Contract
codebase-context.md, feasibility-report.md

## Output Contract
step1/impact-analysis.md

## Requires (capabilities)
code-graph tier3 (find_callers/callees)

## Conditional Firing
false

## Contract
This agent is a declaration only: WHAT it does, WHEN it fires, WHAT it
requires, WHAT it produces. Implementation logic lives entirely in
`skills/step1/impact-analysis/SKILL.md` per the framework's agent/skill
separation (Section 5, Key Design Principle #2).
