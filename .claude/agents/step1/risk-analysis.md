---
agent_id: risk-analysis
category: STEP1
phase: step1
model_tier: sonnet
conditional: false
calls_skill: skills/step1/risk-analysis/SKILL.md
---

# Agent: Risk Analysis

## Purpose
Identifies risks and mitigations across technical, security, and delivery dimensions

## Phase Placement
step1

## Input Contract
feasibility-report.md, impact-analysis.md

## Output Contract
step1/risk-register.md

## Requires (capabilities)
none

## Conditional Firing
false

## Contract
This agent is a declaration only: WHAT it does, WHEN it fires, WHAT it
requires, WHAT it produces. Implementation logic lives entirely in
`skills/step1/risk-analysis/SKILL.md` per the framework's agent/skill
separation (Section 5, Key Design Principle #2).
