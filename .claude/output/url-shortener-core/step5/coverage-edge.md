---
agent: coverage-edge
inputs: [step2/acceptance-criteria.md, step3/api-contract.yaml]
---

# Coverage & Edge Cases

Coverage tier: **full** (per `coverage-strategy`, risk level MEDIUM
from `step1/risk-register.md` didn't warrant risk-weighted's 95%
sub-tier, but full's 80% floor applies).

## Edge Cases Added Beyond the Literal ACs
- Invalid `targetUrl` shape (`LinkControllerIntegrationTest#createWithInvalidUrl_returns400`) — AC-4's negative case.
- Alias collision at the HTTP layer, not just the service layer (`#createWithTakenAlias_returns409`) — confirms the machine-readable `ALIAS_TAKEN` code survives the exception-handler mapping.
- Concurrent creation with 20 simultaneous callers, 8-thread pool (`LinkServiceTest#createLink_concurrentCreation_neverCollides`) — this is the one genuinely load-bearing edge case for R1/AC-9.

## Known Gap (documented, not silently dropped)
`LinkControllerIntegrationTest#redirectExpiredCode_returns410` is a
placeholder — testing the 410 path at the HTTP layer needs a
controllable clock (not part of `technical-design.md`'s scope, which
uses `Instant.now()` directly). The **behavior itself is covered** at
the service layer (`LinkServiceTest#resolveAndRecordClick_expiredCode_throwsExpired`,
using a negative `expiresInDays` to force an already-past `expiresAt`)
— what's missing is only the HTTP-status-code assertion for that same
path. Flagged in `docs/testing-and-limitations.md` rather than left
unstated.
