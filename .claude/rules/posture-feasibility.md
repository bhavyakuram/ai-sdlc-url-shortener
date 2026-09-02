# Rule: Posture Feasibility

**Category:** Feasibility · **Priority:** 3

## Purpose
An always-on (every posture, not just `*-mod`) PRE-WORK check: does the
codebase evidence actually match the posture the operator filed?

Example: operator files `doc` (audit-only) posture, but
`requirement-ingestion` finds language implying new code needs to be
written ("add an endpoint for..."). `posture-feasibility` flags this
mismatch before any further phase runs, rather than discovering it at
Gate 2 after specification work has already been spent.

## Outcome
A mismatch does not auto-fail the run — it surfaces as a flagged
warning at Gate 1, giving the operator the same
RATIFY/EXPAND/NARROW/NO-GO options as `rules/role-feasibility.md`.
