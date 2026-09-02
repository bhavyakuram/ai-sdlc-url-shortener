---
agent_id: architecture-analysis
category: PREWORK
phase: prework
model_tier: sonnet
conditional: false
calls_skill: skills/prework/architecture-analysis/SKILL.md
---

# Agent: Architecture Analysis

## Purpose
Analyzes existing codebase architecture: layers, dependencies, patterns (brownfield) or declares clean-slate baseline (greenfield)

## Phase Placement
prework

## Input Contract
Repo source tree

## Output Contract
shared-context/{stack}/snapshots/{sha}/architecture-context.md + architecture-digest.md

## Requires (capabilities)
code-graph tier2/3

## Conditional Firing
false

## Contract
This agent is a declaration only: WHAT it does, WHEN it fires, WHAT it
requires, WHAT it produces. Implementation logic lives entirely in
`skills/prework/architecture-analysis/SKILL.md` per the framework's agent/skill
separation (Section 5, Key Design Principle #2).
