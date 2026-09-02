---
mode_id: hybrid
default: true
---

# Mode: hybrid (default)

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
The default for this project — enough autonomy to move at a reasonable
pace without giving up the safety of gates at every phase boundary.
