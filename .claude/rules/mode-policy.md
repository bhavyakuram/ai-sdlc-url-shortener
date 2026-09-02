# Rule: Mode Policy

**Category:** Process · **Priority:** 3

## Mode Validation
Exactly one of `deterministic | hybrid | agentic` is active per run,
resolved at launch and frozen for the run's duration (Section 2,
Configuration Axes — mid-run mode change is forbidden).

## Bounded Autonomy Ceiling (applies even in agentic mode)
The `conductor` may never:
- change the active role, stack, or mode mid-run,
- bypass HITL Gates 0-3 (Gates 4/5 may be auto-handled by
  `online-learning` after 10 consistent approvals; Gates 0-3 never
  auto-approve),
- override an organization standard,
- exceed the 2x cost cap enforced by `cost-router`.

## Gate Activation by Mode
See `reference/mode-catalog.md` for the full 8-gate x 3-mode matrix.
Deterministic = all gates always active. Hybrid = Gates 0/0.5/1/2/4/5
always active, Gate 3 active, Gate 6 always active (per-feature
intent, never auto). Agentic = same as hybrid plus online-learning
eligibility on Gates 4/5.
