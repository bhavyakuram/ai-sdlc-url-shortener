# Rule: Role Feasibility

**Category:** Feasibility · **Priority:** 3

## Purpose
Detects, as early as possible, a mismatch between the *filed* role
(e.g. `services-mod`) and the *actual* code surface the feature touches.

## Two-Pass Design
- **Pass 1** (PRE-WORK, `*-mod` postures only): coarse check — does the
  discovered code surface roughly match the active role's declared
  layers?
- **Pass 2** (STEP-1 Stage 4, conditional): refined check using the
  impact-analysis and risk-register data that wasn't available yet at
  Pass 1.

## Gate Behavior
A Pass-2 warning extends Gate 1 with role-confirmation options:
`RATIFY` (proceed as filed) / `EXPAND_LANES` (widen role) /
`NARROW_LANES` (shrink role) / `NO-GO`. `NO-GO` is a hard stop — no
exceptions, per `rules/quality-gates.md`.
