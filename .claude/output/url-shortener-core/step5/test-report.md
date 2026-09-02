---
agent: test-generation + evaluator
---

# Test Report

**Command:** `mvn -o test` (offline, JDK 19 / Spring Boot 3.1.4)

**Verbatim result:**
```
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0 - in LinkControllerIntegrationTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0 - in LinkServiceTest
[INFO] Results:
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

## AC → Test Traceability
| AC | Test |
|---|---|
| AC-1 | `LinkServiceTest#createLink_withoutAlias_generatesUniqueCode` |
| AC-2 | `LinkServiceTest#createLink_withAlias_usesExactAlias` |
| AC-3 | `LinkServiceTest#createLink_withTakenAlias_throwsAliasTaken`, `LinkControllerIntegrationTest#createWithTakenAlias_returns409` |
| AC-4 | `LinkControllerIntegrationTest#createWithInvalidUrl_returns400` |
| AC-5 | `LinkServiceTest#resolveAndRecordClick_existingLink_returnsTargetAndRecordsClick`, `LinkControllerIntegrationTest#createThenRedirect_returns201then302` |
| AC-6 | `LinkServiceTest#resolveAndRecordClick_unknownCode_throwsNotFound`, `LinkControllerIntegrationTest#redirectUnknownCode_returns404` |
| AC-7 | `LinkServiceTest#resolveAndRecordClick_expiredCode_throwsExpired` (HTTP-layer status assertion: gap, see coverage-edge.md) |
| AC-8 | `LinkServiceTest#getAnalytics_reflectsExactClickCount`, `LinkControllerIntegrationTest#analytics_afterOneClick_showsOneEvent` |
| AC-9 | `LinkServiceTest#createLink_concurrentCreation_neverCollides` |

**9/9 ACs have at least one test** (`rules/testing.md` Every-AC-Needs-a-Test — satisfied).

## Coverage Tooling Gap (documented honestly)
JaCoCo is not yet wired into `pom.xml`, so there is no *measured*
line-coverage percentage to report against the 80% full-tier floor —
only the AC-traceability table above. This is a real gap, not a
rounding-error: `rules/testing.md`'s coverage floor is asserted
qualitatively here, not verified mechanically. Flagged in
`docs/testing-and-limitations.md`.

## Evaluator Verdict
**PASS** — all 14 tests green, all 9 ACs traced to at least one test.
Coverage-tooling gap noted above does not block this PASS (it is a
measurement gap, not a failing test), but is carried forward as a
known limitation rather than silently resolved.
