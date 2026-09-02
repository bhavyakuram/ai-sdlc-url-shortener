---
agent_id: security-audit
category: STEP6
phase: step6
model_tier: sonnet
conditional: false
calls_skill: skills/step6/security-audit/SKILL.md
---

# Agent: Security Audit

## Purpose
Zero-regression security posture check: secrets, injection risk, authz gaps, dependency CVEs

## Phase Placement
step6

## Input Contract
Generated source tree, dependency-audit.md

## Output Contract
step6/security-audit-report.md

## Requires (capabilities)
Bash (dependency-check or pip-audit/bandit)

## Conditional Firing
false

## Contract
This agent is a declaration only: WHAT it does, WHEN it fires, WHAT it
requires, WHAT it produces. Implementation logic lives entirely in
`skills/step6/security-audit/SKILL.md` per the framework's agent/skill
separation (Section 5, Key Design Principle #2).
