---
agent: risk-analysis
---

# Risk Register

| # | Risk | Category | Likelihood | Impact | Mitigation |
|---|---|---|---|---|---|
| R1 | Short-code collision under concurrent creation | Technical | Medium | High (data corruption if unhandled) | Unique DB constraint on `short_code` + retry-on-conflict at service layer (per market-research). Test explicitly in STEP-5 coverage-edge. |
| R2 | Custom alias race (two callers request same alias simultaneously) | Technical | Low | Medium | Same unique-constraint mechanism as R1 covers this case too. |
| R3 | Open redirect abuse (short link redirecting to a malicious target submitted at creation) | Security | Medium | Medium | Out of this PRD's stated scope to *block* arbitrary targets (idea.md doesn't ask for URL allow/deny-listing) — flagged as a known limitation for `docs/testing-and-limitations.md`, not silently ignored. |
| R4 | No rate limiting on link creation (Could-have, not built in v1) | Reliability | Medium | Low-Medium | Documented gap; `security-audit` (STEP-6) checks this doesn't regress below documented baseline, doesn't require it be fixed now. |
| R5 | Referrer capture is unreliable (browser-dependent) | Data quality | High | Low | Documented as a known limitation, not a bug — analytics schema tolerates null referrer. |

**Overall risk level: MEDIUM** (driven by R1; nothing here blocks a
GO verdict — all have a mitigation or an explicitly accepted scope
boundary).
