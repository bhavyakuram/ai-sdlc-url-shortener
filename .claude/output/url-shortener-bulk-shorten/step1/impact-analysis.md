# STEP-1: Impact Analysis — url-shortener-bulk-shorten

**Agent:** impact-analysis · **Input:** `prework/prd-v0.md`, `step1/feasibility-report.md`, real
source under `service-java-spring/src/main/java/com/aisdlc/urlshortener/` (re-read directly for
this artifact, not inferred from PRD prose).

## Does the existing single-create endpoint's behavior change? **No.**
`api/LinkController.java`'s existing `@PostMapping("/api/v1/links")` method (lines 36-45),
`service/LinkService.java`'s existing `createLink()` (lines 67-79) and its two private helpers, and
every existing `@ExceptionHandler` in `ApiExceptionHandler.java` are reused **read-only** — called
with the same arguments, same return type, same exception types. Nothing in this feature requires
changing their signature, body, or the status codes/fields they currently produce. `FR-B4` (unchanged
single-create endpoint) holds structurally, not just by intent.

## File-by-file impact

### CHANGED (existing files edited — additive edits only, no existing method touched)
| File | Change | Why it's "changed" not "new" |
|---|---|---|
| `api/LinkController.java` | Add one new method: `@PostMapping("/api/v1/links/batch")`. Existing `createLink`, `redirect`, `stats` methods and the private `buildShortUrl` helper are untouched. | It's an edit to an existing class file, even though no existing method's code changes. |
| `api/ApiExceptionHandler.java` | Add two new `@ExceptionHandler` methods for the two new batch-level 400 codes (`EMPTY_BATCH`, `BATCH_TOO_LARGE` — see prd-v0.md Section 4.4). Existing six handlers untouched. | Same reasoning — additive edit to an existing file. |

### ADDITIVE (brand-new files, existing code untouched)
| File (new) | Purpose |
|---|---|
| `api/dto/BatchCreateLinkRequest.java` (new record) | Wraps `List<CreateLinkRequest> items` — reuses the existing `CreateLinkRequest(url, customCode)` record per item rather than duplicating its two fields, so the request DTO stays a thin wrapper, not a parallel vocabulary. |
| `api/dto/BatchCreateLinkResponse.java` (new record) | `List<BatchItemResult> results`, `int successCount`, `int failureCount` — per prd-v0.md Section 4.3's envelope. |
| `api/dto/BatchItemResult.java` (new record, or a sealed interface with two variants) | Per-item outcome, reusing `LinkResponse`'s success fields and `ErrorResponse`'s `code`/`message` fields verbatim (prd-v0.md Section 4.3/FR-B3) — no new field vocabulary. |
| One new service-layer entry point — **recommended as a new class**, e.g. `service/BulkLinkService.java`, constructor-injected with the existing `LinkService` bean (not a new method inside `LinkService.java` itself) | See `risk-register.md` R-BULK-2 for why: a new method added *inside* `LinkService.java` that self-invokes `this.createLink(...)` in a loop would bypass Spring's AOP transactional proxy (self-invocation is not intercepted), silently changing each item's `@Transactional` semantics. A separate class calling `linkService.createLink(...)` through the injected proxy preserves today's per-call transactional behavior exactly. This is this analysis's one concrete deviation-with-reason from `prd-v0.md`'s "(or a thin new service class delegating to createLink per item)" — it resolves that "or" in favor of the new-class branch, on evidence, not preference. |
| `service/exception/EmptyBatchException.java`, `service/exception/BatchTooLargeException.java` (two new, small exception types, following the exact pattern of the six existing ones in `service/exception/`) | Batch-level (not per-item) validation, thrown once before any item is processed — mirrors the existing exception-per-failure-mode pattern already used for every other validation rule in this codebase. |

### UNAFFECTED (confirmed by direct re-read, not assumed)
| File | Why unaffected |
|---|---|
| `service/LinkService.java` | Not edited at all under the recommended design (new `BulkLinkService` calls it as an external dependency) — reused as a black box, exactly as `prd-v0.md` intends. |
| `data/ShortLinkEntity.java`, `data/ShortLinkRepository.java` | No schema/entity change — confirmed `code` column is `UNIQUE`/`NOT NULL`, one row per link, already sufficient for N independent inserts (re-read verbatim, Section 2.4 of prd-v0.md independently confirmed). |
| `data/ClickEventEntity.java`, `data/ClickEventRepository.java` | Click tracking is a redirect-path concern only; batch-create never calls `redirectAndRecordClick`. |
| `service/CodeGenerator.java` | Reused per item, unchanged — no batch-aware variant needed (confirmed its `encode`/`randomPlaceholder` methods take no batch context). |
| `service/GeoLookupService.java`, `service/RateLimiterService.java` | Redirect-path only; batch-create doesn't call either. `RateLimiterService.tryConsume` is keyed on `(sourceIp, code)`, i.e. resolved short code — meaningless pre-creation, so it structurally cannot apply to a create-time endpoint without new code (see risk-register.md R-BULK-1 for whether a *different*, request-level limiter should be added). |
| `api/RateLimitInterceptor.java`, `api/WebConfig.java` | **Not edited under the recommended STEP-1 disposition** (see risk-register.md R-BULK-1) — the batch endpoint falls under `/api/**`, already excluded by `WebConfig.java:25` (`excludePathPatterns("/api/**", "/actuator/**")`), same as today's single-create endpoint. If Gate 2/3 instead adopts R-BULK-1's recommended new, narrower limiter, that would touch `WebConfig.java`'s interceptor registration (or add a second interceptor) — flagged as a decision point, not decided here. |
| `api/util/ClientIpResolver.java` | Only consumed by `RateLimitInterceptor` and the redirect controller method today; unaffected unless R-BULK-1 is adopted, in which case it would be reused (not modified) by a new limiter. |
| `api/dto/CreateLinkRequest.java`, `api/dto/LinkResponse.java`, `api/dto/ErrorResponse.java`, `api/dto/StatsResponse.java` | Reused/wrapped by the new batch DTOs above, not modified — their existing fields are exactly what's needed (confirmed field-by-field against `prd-v0.md` Section 4.3). |
| `schema.sql`, `application.yml` | No new table/column, no new config property required (Section 5 of `dependency-audit.md`/`feasibility-report.md`). |
| `UrlShortenerApplication.java` | No new bean wiring beyond standard `@Service`/`@RestController` component scanning, already covers any new class added under the existing package tree. |

## Contract impact
`step3/api-contract.yaml` (if it exists yet for this feature — not yet generated at STEP-1) will
need one new path (`POST /api/v1/links/batch`) added at STEP-3; this is expected, additive contract
growth, not a breaking change to the existing three documented endpoints.

## Summary
**Changed:** 2 files (`LinkController.java`, `ApiExceptionHandler.java`), both additive edits only.
**New:** 6-7 files (3 DTOs, 1 new service class, 2 new exception classes).
**Unaffected:** everything else, including all of `data/`, the redirect path, stats path, and the
existing single-create endpoint's own behavior end-to-end.
