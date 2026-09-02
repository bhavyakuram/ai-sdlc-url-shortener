# Rule: Testing

**Category:** Quality · **Priority:** 3

## Every AC Needs a Test
Every `GIVEN/WHEN/THEN` line written by `acceptance-criteria` (STEP-2)
must have at least one corresponding test written by `test-generation`
(STEP-5). `evaluator` cross-references AC ids against test names/tags
and fails the run if any AC has zero coverage.

## Coverage Floor
Minimum line coverage on in-scope files is set by
`coverage-strategy`'s recommended tier (smoke: 60%, full: 80%,
risk-weighted: 80% overall + 95% on the specifically flagged risk
areas). Coverage below the floor is a FAIL, not a warning.

## Test Independence
Tests must not depend on execution order or shared mutable fixtures
across test classes/modules. Flaky-by-design tests (sleeps, wall-clock
assumptions) are rejected by `coverage-edge` review.

## Negative & Edge Cases
`coverage-edge` must add at least one negative test (invalid input,
not-found, conflict) per API endpoint, beyond whatever the literal ACs
specified.
