---
agent: planner
---

# Sprint Plan

## Wave 1 — Scaffold + Data
pom.xml (spring-boot-starter-parent 3.1.4, web, data-jpa, validation, h2,
test + bucket4j_jdk17-core:8.19.0 + com.maxmind.geoip2:geoip2:5.2.0),
application.yml (H2 file-mode, geoip mmdb path, rate-limit config),
UrlShortenerApplication, ShortLinkEntity, ClickEventEntity (+country),
ShortLinkRepository, ClickEventRepository.

## Wave 2 — Service
CodeGenerator (base62 + reserved-word set), GeoLookupService
(MaxMind wrapper, fail-soft on missing/unreadable .mmdb — must not
throw, must not fail the build if the database file is absent at
runtime), RateLimiterService (Bucket4j, keyed by IP+code),
LinkService (create/resolve/stats — Candidate A generation strategy,
single LinkUnavailableException for both never-existed and expired
per feature-spec.md's deliberate non-disambiguation), exception types.

## Wave 3 — API
DTOs (CreateLinkRequest, LinkResponse, StatsResponse, ErrorResponse),
LinkController (3 endpoints), RateLimitInterceptor + WebConfig
(429 enforcement on the redirect path only), ApiExceptionHandler.

## Wave 4 — Tests
Full AC01-AC25 traceability where feasible in this pass; explicit
note for any AC deferred (e.g. anything needing real MaxMind data
files or wall-clock manipulation).
