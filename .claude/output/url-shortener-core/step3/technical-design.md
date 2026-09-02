---
agent: technical-design
inputs: [step2/feature-spec.md, step2/acceptance-criteria.md, shared-context/java-spring/snapshots/greenfield-baseline/architecture-context.md]
---

# Technical Design: URL Shortener Service (java-spring)

## Component Decomposition
```
com.aisdlc.urlshortener
├── api/        LinkController (POST /links, GET /{code}, GET /links/{code}/analytics)
│               dto: CreateLinkRequest, LinkResponse, AnalyticsResponse, ErrorResponse
├── service/    LinkService (create, resolve+recordClick, getAnalytics)
│               CodeGenerator (base62 encode of the persisted id)
└── data/       ShortLinkRepository, ClickEventRepository (Spring Data JPA)
                entity: ShortLinkEntity, ClickEventEntity
```
Matches `layers_in_scope: [api, service, data]` from `_role-context.yaml`
and the dependency direction rule (`rules/architecture.md`: api → service
→ data, never the reverse).

## Technology Choices
- Spring Boot 3.3 (`spring-boot-starter-web`), Spring Data JPA, H2
  (prototype default) — all per `stacks/java-spring/stack-manifest.md`.
- Bean Validation (`@Valid`) on `CreateLinkRequest` for AC-4 (invalid
  target → 400).

## Primitive Selection
- **Short code generation**: persist first with target URL only, get
  the auto-increment `id`, base62-encode it as the code, then update
  the row with its own code in the same transaction. This guarantees
  AC-9 (collision-safety) for free — codes are derived from a
  DB-guaranteed-unique id, no separate uniqueness check/retry loop
  needed for the generated-code path.
- **Custom alias path**: insert with a unique constraint on
  `short_code`; catch the constraint-violation and translate to AC-3's
  `409 ALIAS_TAKEN` — this is the retry-free way to get correctness
  under concurrency (race the DB, don't race in application code).
- **Expiry check**: a plain `expiresAt` timestamp column, checked at
  redirect time (`expiresAt.isBefore(now)` → 410). No background
  expiry sweep needed for v1 — checking at read time is sufficient and
  simpler (fewer moving parts, per `rules/coding-standards.md`).

## API Design
REST + JSON, per `step2/feature-spec.md` FS-1..FS-3 exactly. Full
schema in `api-contract.yaml` (this phase's sibling output).
