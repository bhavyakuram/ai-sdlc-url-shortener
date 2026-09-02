---
mode_id: agentic
---

# Mode: agentic

**Conductor agent orchestrates within a matrix row.** Maximum autonomy,
cost-capped.

## The 6 Agentic Surfaces (additional to hybrid's 6)
1. `conductor` — decision loop sequencing STEP-N to STEP-N+1
2. `transition-fsm` — manages state transitions within the matrix row
3. `parallel-explorer` — spawns up to 3 parallel branches within a phase
4. `cost-router` — hard cap at 2x predicted cost
5. `online-learning` — auto-handles Gates 4 & 5 after 10 consistent
   approvals
6. `adaptive-gate` — calibrates retry budget (3-8) per role lane

## Bounded Autonomy Ceiling
Even here, the conductor cannot change the active role/stack/mode,
bypass Gates 0-3, override standards, or exceed the 2x cost cap — see
`rules/mode-policy.md`.

## When To Use
A stretch-goal demonstration for this assignment (Core Requirement #7,
"Controlled Autonomy") once the hybrid-mode path is proven working —
not the primary path given the 2-3 day budget.
