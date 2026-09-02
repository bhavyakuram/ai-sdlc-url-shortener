---
agent_id: code-graph-bootstrap
category: PREWORK
phase: prework
model_tier: haiku
conditional: false
calls_skill: skills/prework/code-graph-bootstrap/SKILL.md
---

# Agent: Code Graph Bootstrap

## Purpose
Binds the code knowledge-graph MCP provider OR declares text-scan fallback mode for this run

## Phase Placement
prework

## Input Contract
Repo root path, config/mcp/code-graph-provider.yaml

## Output Contract
prework/code-graph-status.md

## Requires (capabilities)
code-graph (optional, falls back to text-grep)

## Conditional Firing
false

## Contract
This agent is a declaration only: WHAT it does, WHEN it fires, WHAT it
requires, WHAT it produces. Implementation logic lives entirely in
`skills/prework/code-graph-bootstrap/SKILL.md` per the framework's agent/skill
separation (Section 5, Key Design Principle #2).
