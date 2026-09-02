---
agent_id: codebase-context
category: PREWORK
phase: prework
model_tier: sonnet
conditional: false
calls_skill: skills/prework/codebase-context/SKILL.md
---

# Agent: Codebase Context

## Purpose
Discovers in-scope files, builds layer map, flags drift-vs-standards

## Phase Placement
prework

## Input Contract
Repo source tree, active role layers_in_scope

## Output Contract
shared-context/{stack}/snapshots/{sha}/codebase-context.md + codebase-digest.md

## Requires (capabilities)
code-graph tier1

## Conditional Firing
false

## Contract
This agent is a declaration only: WHAT it does, WHEN it fires, WHAT it
requires, WHAT it produces. Implementation logic lives entirely in
`skills/prework/codebase-context/SKILL.md` per the framework's agent/skill
separation (Section 5, Key Design Principle #2).
