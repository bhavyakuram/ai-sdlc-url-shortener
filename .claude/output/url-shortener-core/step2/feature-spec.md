---
agent: feature-spec
inputs: [prework/prd-v0.md, step1/feasibility-report.md, step1/risk-register.md]
---

# Feature Spec: URL Shortener Service

## FS-1 Create Short Link
`POST /links` — accepts `{ targetUrl, alias?, expiresInDays? }`.
- `targetUrl`: required, must be a syntactically valid absolute URL.
- `alias`: optional, 3-32 chars, `[a-zA-Z0-9-_]`; if omitted, a base62
  code is generated from the new record's id (per market-research).
- `expiresInDays`: optional positive integer; defaults to 90 (concept.md).
- Returns `201` with the created `ShortLink` (code, target, expiresAt).
- Returns `409` if `alias` is already in use.
- Returns `400` if `targetUrl` is missing/invalid.

## FS-2 Redirect
`GET /{code}` — 302 redirect to the target URL if the link exists and
has not expired. Records a `ClickEvent` (timestamp + `Referer` header
if present) before responding.
- Returns `404` if the code doesn't exist.
- Returns `410 Gone` if the code exists but is expired (distinct from
  404 — the caller should be able to tell "never existed" from "existed,
  now gone," which the idea.md didn't ask for explicitly but is a
  natural, low-cost refinement of "some kind of expiry").

## FS-3 Analytics
`GET /links/{code}/analytics` — returns click count + the click-event
log (timestamp, referrer-or-null) for that link.
- Returns `404` if the code doesn't exist (expired links still return
  their historical analytics — expiry stops redirecting, not reporting).

## FS-4 No Authentication
No endpoint in this spec requires a credential (idea.md: anonymous v1).
Every endpoint is intentionally public, per the accepted R3/R4 risk
scope.
