# Rule: Retry, Rollback & Safe-Stop

**Category:** Process · **Priority:** 3
**Status: NEW — added specifically to satisfy the assignment's explicit
"bounded retries, fallback, rollback, and safe-stop controls"
requirement, extending `retry-policy.md`'s retry + Gate-4 waiver with
genuine rollback and safe-stop outcomes. This file follows the same
conventions as the rest of `rules/`.**

## Three Distinct Outcomes on Repeated Failure
1. **Retry** (existing, `retry-policy.md`): re-attempt with explicit
   feedback, consuming one retry-budget unit.
2. **Rollback** (new): when a retry itself fails catastrophically
   (build-verdict BLOCKER on a wave that previously compiled, or
   evaluator regresses previously-passing tests), the generator's
   current wave is reverted — `git checkout -- <wave files>` back to
   the last commit boundary written by the prior successful wave —
   before the next retry attempt. Rollback is logged in `_run-log.md`
   with the commit SHA reverted to.
3. **Safe-Stop** (new): when a role lane exhausts its retry budget
   (`retry-policy.md` hard limit) without a PASS, the run does not loop
   forever or fail silently. It:
   - rolls back to the last known-good commit for that lane,
   - marks the run `SAFE-STOPPED` in `_run-log.md` and
     `_reliability-metrics.json`,
   - surfaces at Gate 4 with the full failure history attached, and
   - takes no further generation action until the operator chooses
     `accept-and-release (waiver)` / `route-back-to-STEP-4 (fresh
     budget)` / `abort`.

## Invariant
Safe-stop never leaves the working tree in a partially-generated,
non-compiling state. Rollback runs *before* safe-stop is declared, not
after.
