---
mode_id: hybrid
---

# Mode: hybrid

**CLAUDE.md routing + 6 bounded decision surfaces.** Balanced autonomy
with safety.

## The 6 Hybrid Decision Surfaces
1. `triage` — classifies feature shape, recommends role
2. `hybrid-router` — adjusts downstream phase weighting by risk
3. `coverage-strategy` — recommends test coverage tier
4. `evaluator-fail-router` — routes failures intelligently instead of
   always cycling STEP-5 to STEP-4
5. `confidence-scorer` — emits high/medium/low confidence on every gate
   output
6. `planner-escalation` — on second retry, decides split-vs-simplify

## HITL Gates
Gates 0, 0.5, 1, 2, 4, 5 always active. Gate 3 active (skippable only
for consumer-only roles with `api_contract_integration` inactive).
Gate 6 always active, never auto.

## When To Use
A safer, simpler fallback when agentic mode's added surfaces (conductor
sequencing, parallel exploration, cost capping) aren't wanted — e.g. a
first run against a brand-new stack. Not the default for this project;
see `modes/agentic/mode-manifest.md`.
