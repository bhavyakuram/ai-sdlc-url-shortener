---
agent: technical-design
inputs: [step2/feature-spec.md, step2/acceptance-criteria.md, step3/parallel-explorer-candidates.md, step1/feasibility-report.md]
---

# Technical Design: URL Shortener Service (java-spring)

## Component Decomposition
```
com.aisdlc.urlshortener
├── api/        LinkController (3 endpoints), dto/, ApiExceptionHandler, RateLimitFilter (or interceptor)
├── service/    LinkService (create/resolve/stats), CodeGenerator, GeoLookupService, RateLimiterService
└── data/       ShortLinkRepository, ClickEventRepository (Spring Data JPA)
```
`layers_in_scope: [api, service, data]`, dependency direction
api → service → data.

## Short-Code Generation — Candidate A (parallel-explorer)
See `step3/parallel-explorer-candidates.md`. Generated-code path:
insert with placeholder → base62(id) → update, one transaction.
Custom-code path (including reserved-word check from feature-spec.md
Section 5): insert directly with unique constraint on `code`, catch
`DataIntegrityViolationException` → 409.

## Rate Limiting (Bucket4j, per step1's dependency choice)
`com.bucket4j:bucket4j_jdk17-core:8.19.0`, in-process bucket keyed by
(source IP, code), 100 req/min, 429 beyond that — applied as a
`HandlerInterceptor` on the redirect endpoint only (create/stats are
not the abuse surface named in the PRD).

## Geo Lookup (MaxMind GeoIP2, per step1's dependency choice)
`com.maxmind.geoip2:geoip2:5.2.0` + bundled `GeoLite2-Country.mmdb`,
country-level only. **Fail-soft** (AC20): a lookup failure (missing
DB, malformed IP) must not fail the redirect — logs and proceeds with
`country=null`, consistent with the click-write-isolation pattern
this project has used before for non-critical side effects.

## Persistence (H2 file-mode, per step0's A4 decision)
`jdbc:h2:file:./data/urlshortener;` — not in-memory. H2 console
explicitly disabled outside local dev (risk register R-3: console
left enabled = unauthenticated RCE given this service has no auth
layer anywhere).

## Expiry
`expiresAt` timestamp, checked at read time, 30-day default (A1) →
404 on redirect (not 410 — feature-spec.md Section 4), stats endpoint
still queryable after expiry (AC24).
