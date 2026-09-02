---
agent: requirement-ingestion
inputs: [inputs/url-shortener-core/ideation/idea.md, step0/concept.md]
---

# PRD v0: URL Shortener Service

## Normalized Requirement
Build a REST service (java-spring) that:
1. Accepts a long URL and returns a short link (`POST /links`).
2. Redirects a short code to its target URL (`GET /{code}`) with a
   302, preserving click-tracking (per market-research's redirect
   mechanism finding).
3. Supports an optional caller-supplied custom alias, rejecting with
   409 on collision.
4. Applies a default 90-day expiry, overridable per-link at creation.
5. Records a click-event log (timestamp + best-effort referrer) per
   link, retrievable via an analytics endpoint.
6. Requires no authentication (anonymous, v1).

## Source Traceability
Every clause above traces to either idea.md directly or to a Gate-0-approved
proposal in `step0/concept.md`'s "Flagged Ambiguities" section — no
requirement in this PRD is invented without a source.

## Explicitly Out of Scope (v1)
Accounts/login, custom domains, bulk import/export, rate limiting
(Could-have, deferred pending STEP-1 risk assessment).
