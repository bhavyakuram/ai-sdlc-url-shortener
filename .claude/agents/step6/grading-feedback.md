---
agent_id: grading-feedback
category: STEP6
phase: step6
model_tier: sonnet
conditional: false
calls_skill: skills/step6/grading-feedback/SKILL.md
---

# Agent: Grading & Feedback

## Purpose
Produces the final quality score (0.0-1.0) and PASS/FAIL verdict; PASS (>=0.8) completes the feature, FAIL routes to STEP-4 or Gate 4 waiver

## Phase Placement
step6

## Input Contract
static-analysis-report.md, security-audit-report.md, test-report.md

## Output Contract
step6/grading-report.md

## Requires (capabilities)
none

## Conditional Firing
false

## Contract
This agent is a declaration only: WHAT it does, WHEN it fires, WHAT it
requires, WHAT it produces. Implementation logic lives entirely in
`skills/step6/grading-feedback/SKILL.md` per the framework's agent/skill
separation (Section 5, Key Design Principle #2).
