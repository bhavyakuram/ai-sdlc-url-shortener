---
agent: risk-analysis
---

# Risk Register

| # | Risk | Category | Likelihood | Impact | Note |
|---|---|---|---|---|---|
| R1 | A transient DB error during a click write surfaces as a 500 to the *redirect* caller (not just failing analytics) — the redirect itself fails, not just the click count | Reliability | Low | Medium | This is real, but it's a *visible-failure* risk, not the *silent-loss* risk the raw request worried about. Worth naming as a legitimate, different, smaller finding. |
| R2 | No retry/backoff on transient DB errors anywhere in the write path | Reliability | Low | Low | Pre-existing characteristic of the whole service (also true of `createLink`), not specific to analytics. |

**Overall risk level: LOW.** Neither risk matches "quietly losing
data." R1 is arguably worth a small hardening change (don't let a
click-recording failure fail the redirect) — this is the one place
where EXPAND_LANES (allowing a code change) would have a concrete,
scoped thing to do, rather than nothing.
