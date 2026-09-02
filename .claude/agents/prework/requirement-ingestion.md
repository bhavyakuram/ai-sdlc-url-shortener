---
agent_id: requirement-ingestion
category: PREWORK
phase: prework
model_tier: sonnet
conditional: false
calls_skill: skills/prework/requirement-ingestion/SKILL.md
---

# Agent: Requirement Ingestion

## Purpose
Reads the raw requirement plus any supporting docs; produces a normalized PRD v0

## Phase Placement
prework

## Input Contract
inputs/{feature-id}/(ideation|jira|supporting-docs)

## Output Contract
prework/prd-v0.md

## Requires (capabilities)
mcp:jira (optional)

## Conditional Firing
false

## Contract
This agent is a declaration only: WHAT it does, WHEN it fires, WHAT it
requires, WHAT it produces. Implementation logic lives entirely in
`skills/prework/requirement-ingestion/SKILL.md` per the framework's agent/skill
separation (Section 5, Key Design Principle #2).
