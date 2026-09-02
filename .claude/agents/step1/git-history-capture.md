---
agent_id: git-history-capture
category: STEP1
phase: step1
model_tier: haiku
conditional: mod+incident-fix only
calls_skill: skills/step1/git-history-capture/SKILL.md
---

# Agent: Git History Capture

## Purpose
*-mod + incident-fix only: analyzes git history for hotspots, co-change clusters, regression markers

## Phase Placement
step1

## Input Contract
git log of in-scope files

## Output Contract
step1/git-history.md

## Requires (capabilities)
Bash(git log/blame)

## Conditional Firing
mod+incident-fix only

## Contract
This agent is a declaration only: WHAT it does, WHEN it fires, WHAT it
requires, WHAT it produces. Implementation logic lives entirely in
`skills/step1/git-history-capture/SKILL.md` per the framework's agent/skill
separation (Section 5, Key Design Principle #2).
