# Rule: Retry Policy

**Category:** Process · **Priority:** 3

## Hard Limits
- Max 5 retries per feature **per role lane** (default; `adaptive-gate`
  may calibrate between 3-8 based on historical pass rate for the
  stack+role combination, never exceeding this file's ceiling).
- Each retry MUST address the explicit feedback from the failing gate —
  silent re-execution of the identical prior attempt is rejected.
- The retry counter is tracked per role lane independently: in a
  `fullstack` composite run, `services-dev` and `frontend-dev` lanes
  have independent budgets.
- Cache reuse (a shared-context hit) is NOT a retry — it costs zero
  retry budget.
- Resuming from a partial failure (e.g. 3 of 5 files generated before a
  crash) consumes exactly ONE retry slot, regardless of file count.

## The Loop
```
STEP-4 -> STEP-4.1 (Build Verdict) -- FAIL --> back to generator (consumes 1 retry)
                    -- PASS --> STEP-5 (Validation) -- FAIL --> back to STEP-4 (consumes 1 retry)
                                       -- PASS --> STEP-6 (Audit) -- FAIL --> back to STEP-4 or Gate 4 waiver
                                                          -- PASS --> COMPLETE
```

## Exhaustion
When a role lane exhausts its retry budget without a PASS, the run does
**not** silently fail: it invokes `rules/retry-rollback-safestop.md`.
