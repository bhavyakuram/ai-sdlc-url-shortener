---
agent_id: triage
category: PREWORK
phase: prework
model_tier: haiku
conditional: false
calls_skill: skills/prework/triage/SKILL.md
---

# Agent: Triage

## Purpose
Classifies feature shape (greenfield, enhancement, incident-fix, refactor); recommends role; sets retry budget

## Phase Placement
prework

## Input Contract
Raw feature request (idea.md or inputs/{feature-id}/jira export)

## Output Contract
prework/triage-verdict.md (feature_shape, recommended_role, retry_budget)

## Requires (capabilities)
none

## Conditional Firing
false

## Contract
This agent is a declaration only: WHAT it does, WHEN it fires, WHAT it
requires, WHAT it produces. Implementation logic lives entirely in
`skills/prework/triage/SKILL.md` per the framework's agent/skill
separation (Section 5, Key Design Principle #2).
