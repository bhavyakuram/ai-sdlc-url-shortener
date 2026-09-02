---
agent: feature-spec
---

# Feature Spec: Click-Recording Failure Isolation (R1 fix)

## FS-6 Redirect must not fail because click-recording failed
`GET /{code}` (FS-2, unchanged contract) must still return its `302`
even if writing the `ClickEventEntity` fails for any reason. A failed
click write is logged (per `rules/coding-standards.md` Logging — every
service-layer method that can fail logs the failure with context) but
never surfaces as an error to the redirect caller. Analytics undercounting
in this rare case is an accepted, documented trade-off — a failed
redirect for a transient analytics-write blip is a worse user-facing
outcome than an undercounted click.
