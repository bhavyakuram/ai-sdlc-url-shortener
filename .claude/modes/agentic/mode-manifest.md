---
mode_id: agentic
default: true
---

# Mode: agentic (default)

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
**The default and primary path for this project.** The assignment's
Core Requirement #7 ("Controlled Autonomy: Agents execute multi-step
work; humans provide oversight, approvals, and final quality control")
and its repeated emphasis on "agentic" orchestration call for this mode
specifically, not hybrid's more conservative posture — hybrid remains
available as a documented fallback (see `modes/hybrid/mode-manifest.md`)
but is not what this project demonstrates by default.
