---
agent_id: ux-design
category: STEP2
phase: step2
model_tier: sonnet
conditional: false
calls_skill: skills/step2/ux-design/SKILL.md
---

# Agent: UX Design

## Purpose
Defines user flows, screen/response transitions, interaction patterns (reads Figma exports if present)

## Phase Placement
step2

## Input Contract
feature-spec.md, inputs/{feature-id}/figma/

## Output Contract
step2/ux-flow.md

## Requires (capabilities)
mcp:figma (optional)

## Conditional Firing
false

## Contract
This agent is a declaration only: WHAT it does, WHEN it fires, WHAT it
requires, WHAT it produces. Implementation logic lives entirely in
`skills/step2/ux-design/SKILL.md` per the framework's agent/skill
separation (Section 5, Key Design Principle #2).
