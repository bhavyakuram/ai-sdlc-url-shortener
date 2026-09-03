---
agent: test-generation + evaluator
---

3 new tests: AC41 (forced failure via Mockito, redirect still succeeds), AC42
(saveAndFlush verified via Mockito.verify, not just "no exception"), and a
regression guard (LinkUnavailableException still propagates for unknown codes).

**53/53 tests pass** (3 new + all 50 pre-existing, zero regression),
1 documented skip carried forward (AC18). Coverage 91.6%.

**Verdict: PASS.**
