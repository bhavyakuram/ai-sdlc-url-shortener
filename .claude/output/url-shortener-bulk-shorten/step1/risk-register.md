---
agent: risk-analysis
---

# Risk Register

| # | Risk | Category | Likelihood | Impact | Mitigation |
|---|---|---|---|---|---|
| R1 | Unbounded batch size -> large request / DoS-ish load on an unauthenticated endpoint | Reliability/Security | Medium | Medium | Cap at 20 items/request (proposed in prd-v0.md), reject with 400 if exceeded |
| R2 | Partial-failure semantics surprise a caller expecting all-or-nothing | Product | Low | Low | Explicit per-item result shape in the response, documented in feature-spec.md — not left implicit |
| R3 | N sequential DB inserts inside one request could be slow for N=20 | Performance | Low | Low | 20 is small enough that sequential is fine for v1; batching/async explicitly out of scope, noted as a future optimization if batch size ever grows |

**Overall risk level: LOW** — all three have a direct, already-proposed mitigation.

## Role-Feasibility Pass 2
Re-confirmed with impact-analysis data: still MATCH. No new layer
touched beyond what Pass 1 found.
