---
agent_id: build-verdict
category: STEP4
phase: step4
model_tier: haiku
conditional: false
calls_skill: skills/step4/build-verdict/SKILL.md
---

# Agent: Build Verdict (Step 4.1)

## Purpose
Reads compiler/language-server diagnostics to verify the generated code compiles; classifies findings into BLOCKER/HIGH/MEDIUM/LOW severity bands

## Phase Placement
step4

## Input Contract
Generated source tree

## Output Contract
step4/build-verdict.md

## Requires (capabilities)
Bash (mvn/gradle or pytest --collect-only, mypy)

## Conditional Firing
false

## Contract
This agent is a declaration only: WHAT it does, WHEN it fires, WHAT it
requires, WHAT it produces. Implementation logic lives entirely in
`skills/step4/build-verdict/SKILL.md` per the framework's agent/skill
separation (Section 5, Key Design Principle #2).
