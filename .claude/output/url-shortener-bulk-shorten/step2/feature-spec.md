# STEP-2 Specification: Feature Spec — url-shortener-bulk-shorten

**Phase:** STEP-2 · **Agent:** `feature-spec` (skill: `skills/step2/feature-spec/SKILL.md`)
**Stack:** java-spring · **Role:** services-mod (`layers_in_scope: [api, service]`) · **Mode:** agentic
· **Platform:** none
**Inputs read (full):** `prework/prd-v0.md` (FR-B1..B4, Section 4.3/4.4 resolutions),
`step1/feasibility-report.md` (F1/F2 build-order and self-invocation findings),
`step1/impact-analysis.md` (file-by-file design — new `BulkLinkService` class, not a method inside
`LinkService`), `step1/risk-register.md` (R-BULK-1..5, all Gate-1 GO per `_decisions.yaml`),
`url-shortener-core/step2/feature-spec.md` (ground-truth field/error-code vocabulary — reused
verbatim throughout, never reinvented)

---

## 0. Ground-Truth Vocabulary Carried Forward (no reinvention)

Per the task brief and `rules/architecture.md` Technology Agnosticism's "no duplicate vocabulary for
the same concept" spirit, every field and error `code` below is copied verbatim from
`url-shortener-core/step2/feature-spec.md` — this document adds exactly two new error codes
(`EMPTY_BATCH`, `BATCH_TOO_LARGE`, Gate-1-approved) and zero new success-field names.

| Reused from core spec | Value |
|---|---|
| Request fields | `url`, `customCode` (`api/dto/CreateLinkRequest.java`) |
| Success fields | `shortCode`, `shortUrl`, `longUrl`, `createdAt`, `expiresAt` (`api/dto/LinkResponse.java`) |
| Failure fields | `code`, `message` (`api/dto/ErrorResponse.java`'s per-field vocabulary, not its full envelope — see Section 3.1) |
| Per-item error codes reusable as-is | `INVALID_URL_SCHEME`, `URL_TOO_LONG`, `URL_MALFORMED`, `INVALID_CUSTOM_CODE_SHAPE`, `RESERVED_CODE`, `CUSTOM_CODE_TAKEN`, `CODE_SPACE_EXHAUSTED` (all thrown by the reused `LinkService.createLink()`, core spec Section 1.1) |
| Rate-limit code reused (new scope, same token) | `RATE_LIMITED` (core spec Section 6) — see Section 5 below for why no third new code was needed |

---

## 1. API Conventions Addendum

| Convention | Decision | Rationale / trace |
|---|---|---|
| Path | `POST /api/v1/links/batch` — nested under the existing `/api/v1/links` resource, distinct sub-path from the single-create `POST /api/v1/links` | Standard REST convention for a batch variant of an existing collection-create endpoint; keeps the single-create route completely untouched (FR-B4). |
| Request/response encoding | `application/json`, same as `POST /api/v1/links` | Consistency with the existing `api` layer convention (core spec Section 1). |
| Top-level HTTP status | **Always `200 OK`** once request-level (batch) validation passes — per-item outcomes never promote to a top-level 4xx/5xx | `prd-v0.md` Section 4.3, Gate-1-ratified partial-success model. This is the one place this endpoint's status-code convention deliberately diverges from the single-create endpoint's per-request 201/4xx/5xx convention — the divergence is structural (one request now maps to N independent outcomes, not one), not stylistic. |
| Field naming | `camelCase`, identical rule to core spec Section 1 | No new convention needed. |
| Timestamps | ISO-8601 UTC, identical to core spec Section 1 | `createdAt`/`expiresAt` per successful item use the exact same format `LinkResponse` already emits. |
| Auth | None | FR-6 (core spec) applies unchanged — this is still an unauthenticated API. |

### 1.1 Error `code` Vocabulary Addendum

| `code` | HTTP status | Level | Emitted by |
|---|---|---|---|
| `EMPTY_BATCH` | 400 | whole-request | `POST /api/v1/links/batch` — `items` missing, `null`, or `[]` |
| `BATCH_TOO_LARGE` | 400 | whole-request | `POST /api/v1/links/batch` — `items.length > 100` |
| `RATE_LIMITED` | 429 | whole-request | `POST /api/v1/links/batch` — **new scope**: batch-request-level limiter, Section 5 (reuses the existing token, does not reuse the existing limiter instance/key) |
| `INVALID_URL_SCHEME`, `URL_TOO_LONG`, `URL_MALFORMED`, `INVALID_CUSTOM_CODE_SHAPE`, `RESERVED_CODE`, `CUSTOM_CODE_TAKEN`, `CODE_SPACE_EXHAUSTED` | *(varies, see core spec Section 1.1)* | **per-item**, never whole-request | `POST /api/v1/links/batch` — surfaced inside `results[i]`, never as the request's own HTTP status (Section 3, Section 5 of `feasibility-report.md` Finding F1) |
| `MALFORMED_REQUEST` | 400 | whole-request | `POST /api/v1/links/batch` — unreadable/unparseable JSON body, existing catch-all reused unchanged |
| `INTERNAL_ERROR` | 500 | whole-request | `POST /api/v1/links/batch` — unhandled exception, existing catch-all reused unchanged |

The distinction between "whole-request" and "per-item" is the single most important new rule this
spec introduces: **all seven of `createLink()`'s existing exception types must be caught inside the
per-item loop and mapped to a `results[i]` entry — none of them may be allowed to propagate to
`ApiExceptionHandler`**, or the partial-success model silently collapses to all-or-nothing on the
first failing item (`feasibility-report.md` Finding F1, `risk-register.md` R-BULK-4). This is a
correctness constraint on `generator`, encoded here so STEP-4 does not have to rediscover it.

---

## 2. Endpoint Inventory (this feature's addition)

| # | Method | Path | Purpose | FR trace |
|---|---|---|---|---|
| 4 | `POST` | `/api/v1/links/batch` | Submit up to 100 URLs (each optionally paired with a `customCode`) in one request; receive one per-item result per input item, in input order | FR-B1, FR-B2, FR-B3 |

This is the complete addition — endpoints #1-3 (`POST /api/v1/links`, `GET /{code}`,
`GET /api/v1/links/{code}/stats`) are `url-shortener-core/step2/feature-spec.md` Section 2, unchanged
(FR-B4).

---

## 3. Endpoint Specification

### 3.1 `POST /api/v1/links/batch` — Batch Create Short Links

**Request**
```
POST /api/v1/links/batch
Content-Type: application/json

{
  "items": [
    { "url": "https://example.com/a", "customCode": "alias1" },
    { "url": "not-a-url" },
    { "url": "https://example.com/c", "customCode": "alias1" }
  ]
}
```

| Field | Type | Required | Validation | Trace |
|---|---|---|---|---|
| `items` | array of `{url, customCode?}` | yes | Non-empty (else `EMPTY_BATCH`); length <= 100 (else `BATCH_TOO_LARGE`). Both checks run **before any item is processed** — Section 4, R-BULK-5. | FR-B1, prd-v0.md Section 4.4 |
| `items[].url` | string | yes | Identical rule to core spec Section 3.1's `url` field — reused verbatim via `LinkService.createLink()`, not re-specified here | FR-3 (core), FR-B3 |
| `items[].customCode` | string | no | Identical rule to core spec Section 3.1's `customCode` field — reused verbatim | FR-8 (core), FR-B3 |

**Whole-request validation failures (checked before the rate limiter's per-item cost and before any
DB work — see Section 4 for exact ordering)**

| Status | `code` | Condition |
|---|---|---|
| 400 | `EMPTY_BATCH` | `items` is absent, `null`, or an empty array |
| 400 | `BATCH_TOO_LARGE` | `items.length > 100` |
| 400 | `MALFORMED_REQUEST` | Request body is not parseable JSON at all (existing catch-all) |
| 429 | `RATE_LIMITED` | Source IP has exceeded the batch-scoped limiter (Section 5) |

**Success — `200 OK`** (always, once the four whole-request checks above pass — regardless of how
many individual items subsequently fail)
```json
{
  "results": [
    { "status": "CREATED", "shortCode": "abc1234", "shortUrl": "https://<host>/abc1234",
      "longUrl": "https://example.com/a", "createdAt": "2026-09-03T10:00:00Z",
      "expiresAt": "2026-10-03T10:00:00Z" },
    { "status": "FAILED", "code": "URL_MALFORMED", "message": "url is not a well-formed URI" },
    { "status": "FAILED", "code": "CUSTOM_CODE_TAKEN", "message": "Custom code already in use: alias1" }
  ],
  "successCount": 1,
  "failureCount": 2
}
```

| Field | Type | Notes |
|---|---|---|
| `results` | array | Exactly `items.length` entries, **same order as the request** (FR-B2) — `results[i]` is the outcome of `items[i]` and only `items[i]`, positionally correlated, no client-supplied id needed. |
| `results[i].status` | `"CREATED"` \| `"FAILED"` | Discriminator; the only new field not lifted directly from `LinkResponse`/`ErrorResponse`. |
| `results[i]` on `CREATED` | `shortCode`, `shortUrl`, `longUrl`, `createdAt`, `expiresAt` | Identical fields/semantics to core spec Section 3.1's `201` body — same rules (echoed custom code, or 7-char generated code; `expiresAt` = `createdAt` + 30 days). |
| `results[i]` on `FAILED` | `code`, `message` | Identical vocabulary to core spec Section 1.1/3.1's error `code`s — one of `INVALID_URL_SCHEME`, `URL_TOO_LONG`, `URL_MALFORMED`, `INVALID_CUSTOM_CODE_SHAPE`, `RESERVED_CODE`, `CUSTOM_CODE_TAKEN`, `CODE_SPACE_EXHAUSTED`. Note `CODE_SPACE_EXHAUSTED` is a per-item outcome here, never a whole-batch `503` — Section 1.1. |
| `successCount` | int | Count of `results[]` entries with `status: "CREATED"`. |
| `failureCount` | int | Count of `results[]` entries with `status: "FAILED"`. `successCount + failureCount == items.length` always. |

**Per-item processing rule (correctness constraint, not optional):** each `items[i]` is processed by
calling the existing, unmodified `LinkService.createLink(url, customCode)` — reused as a black box,
exactly as `prd-v0.md` Section 4.3 and `impact-analysis.md`'s recommended design specify. The call
**must** go through the injected `LinkService` bean reference (i.e., from a new class — recommended
`service/BulkLinkService.java` — or the controller — holding `LinkService` as a constructor-injected
dependency), never as a self-invocation (`this.createLink(...)`) from a new method added inside
`LinkService.java` itself. This is `risk-register.md` R-BULK-2's mitigation, restated here as a
binding spec-level constraint per that finding's own recommended action ("STEP-3 Technical Design
should encode this as an explicit constraint on the generator, not leave it to implementation-time
judgment") — STEP-2 encodes it now so STEP-3 only has to carry it forward, not originate it.

**Transactional isolation consequence:** because each item's `createLink()` call keeps its own
`@Transactional` boundary (guaranteed only when called through the proxy, per the constraint above),
one item's failure — a thrown, caught-in-loop exception — **never** rolls back, blocks, or otherwise
affects any other item's already-committed or yet-to-be-attempted persistence. A batch of
`[good, bad, good]` persists exactly two rows, regardless of processing order, and regardless of
which item fails or why.

---

## 4. Design Decision: Whole-Request Validation Ordering

**Decision:** for a single `POST /api/v1/links/batch` request, checks run in this fixed order, each
one short-circuiting the rest if it fails:

1. **Rate-limit check** (Section 5) — source-IP-only, runs in a `HandlerInterceptor.preHandle`
   *before* Spring MVC deserializes the request body, so it costs a bucket-map lookup only, never a
   JSON parse or DB round-trip, mirroring core spec Section 6's "fail fast, cheap" precedent for the
   redirect limiter.
2. **Body deserialization** — if the body is unparseable JSON, `MALFORMED_REQUEST` (existing
   catch-all, unchanged).
3. **`items` non-empty / `items.length <= 100`** — `EMPTY_BATCH` / `BATCH_TOO_LARGE`. Runs once,
   before the per-item loop starts; zero items are processed and zero DB round-trips occur for a
   request that fails this check.
4. **Per-item loop** — only entered once steps 1-3 all pass.

**Why this order, not some other:** `risk-register.md` R-BULK-5 flags exactly this ordering risk —
"getting the order wrong... would waste DB work on a request that's going to be rejected anyway, and
— combined with R-BULK-1's absence of a request throttle — makes a malformed/oversized-batch flood
cheaper to send than to reject if checked late." Putting the rate limiter first (step 1) closes that
gap directly: even a flood of maximally-oversized (or empty) batch bodies is capped by the same
limiter that caps everything else on this endpoint, and the cheapest possible rejection path (bucket
check) always runs before any parsing or validation cost is spent.

---

## 5. Design Decision: New, Batch-Scoped Rate Limiter (R-BULK-1 mitigation)

**Threshold: 20 requests per rolling 60-second window, per source IP.**

| Property | Value |
|---|---|
| Scope | `POST /api/v1/links/batch` only. Does **not** apply to, reuse, or reopen the existing `GET /{code}` limiter (`RateLimitInterceptor`, core spec Section 6, FR-9) — that limiter's registration (`WebConfig.java:24-26`, `addPathPatterns("/*")`, `excludePathPatterns("/api/**", ...)`) is untouched by this feature. |
| Key | Source IP only (via the existing, reused `api/util/ClientIpResolver.java` — no new IP-resolution logic needed). Unlike the redirect limiter's `(IP, code)` key, there is no short code to key on pre-creation — a batch request has no single "resource" until after it is processed. |
| Limit | **20 requests / rolling 60-second window**, per source IP |
| Mechanism | A **new**, second `HandlerInterceptor` (e.g. `BatchRateLimitInterceptor`), registered in `WebConfig.java` with `addPathPatterns("/api/v1/links/batch")` only — a distinct registration from, not a modification of, the existing interceptor's `/*`/`excludePathPatterns` rule. Built on **Bucket4j 8.19.0**, already a project dependency (`dependency-audit.md` confirms zero new library needed), reusing the same battle-tested token-bucket approach as the redirect limiter rather than inventing a second throttling mechanism. |
| Storage bound | Bucket map must be bounded/expiring, same precedent as core spec Section 6's storage-bound requirement (R-8-equivalent hygiene) — not an unbounded `ConcurrentHashMap`, even though no risk-register item names this explicitly for the batch endpoint; applying the existing project pattern here is a straightforward consistency call, not a new judgment. |
| Over-limit response | `429 Too Many Requests`, reusing the existing `RATE_LIMITED` code and error-envelope shape verbatim (core spec Section 6) — **no new error code was needed or approved for this** (Gate-1-ratified: two new codes total, `EMPTY_BATCH`/`BATCH_TOO_LARGE`, neither of which is this). `Retry-After`, `X-RateLimit-Limit: 20`, `X-RateLimit-Remaining` headers follow the same convention as the redirect limiter's response headers. |
| Failure mode | Must fail safe — return `429`, never 5xx, identical requirement to core spec Section 6. |

**Justification for 20 req/60s/IP (a judgment call, flagged per the same "use your judgment and flag
it" instruction the original PRD's batch-size cap was flagged under):**

1. **Converts an unbounded amplification into a bounded one, without being so tight it blocks
   legitimate bulk-import use.** Pre-mitigation, `risk-register.md` R-BULK-1 measured the exposure as
   "up to 100x amplification... for the same number of HTTP requests and the same absence of any
   limiter." A cap of 20 requests/minute converts that from *unbounded* request-rate x 100-item
   amplification to a hard ceiling of `20 x 100 = 2,000` link-creation attempts per source IP per
   minute — still generous enough that a real caller importing, say, a 2,000-URL CSV in twenty
   100-item batches completes in about a minute without ever being throttled, but no longer
   unbounded.
2. **Sized against the endpoint's actual per-request cost, not copied from the redirect limiter's
   100/60s.** `risk-register.md` R-BULK-3 establishes that a worst-case batch request can drive **up
   to 200 sequential synchronous DB round-trips** (100 items x up to 2 `saveAndFlush` calls each for
   generated-code items) while holding one Tomcat worker thread for the whole duration — a
   fundamentally more expensive unit of "one request" than the redirect endpoint's single DB read.
   Reusing the redirect limiter's 100/60s threshold verbatim would under-protect the batch endpoint
   by roughly the same 100x factor R-BULK-1 is trying to close; 20/60s — five times tighter than the
   redirect number — reflects that a batch request is a categorically heavier unit of work, sized
   specifically against R-BULK-3's evidence rather than borrowed by analogy.
3. **Bounds worker-thread exhaustion, addressing R-BULK-3's secondary concern.** With the limiter in
   place, at most 20 concurrent-or-sequential batch requests per source IP can be in flight against
   the DB per rolling minute, each individually already bounded to <=100 items by the existing
   whole-request size cap — keeping a single abusive/misbehaving client from monopolizing enough
   Tomcat worker threads to degrade the shared thread pool the redirect path also depends on
   (R-BULK-3's stated cross-endpoint availability concern).
4. **No existing numeric precedent to instead borrow, same reasoning as `prd-v0.md` Section 4.4's
   100-item cap.** Nothing in this codebase already limits request *rate* on a create-type endpoint —
   the redirect limiter's 100/60s governs a different resource (reads against an already-created
   resource, keyed per-code) for a different purpose (per-link abuse, not aggregate write load), so
   it is not treated as binding precedent, only as the source of the token-bucket mechanism and
   header conventions to reuse for consistency.
5. **Explicitly flagged, per this task's own instruction, as a judgment call with no
   requirement-level backing** — if real bulk-import traffic patterns turn out to need a higher
   ceiling (or a per-caller allowance mechanism, which would need an auth concept this API doesn't
   have per FR-6), that is a Gate 2/3-level revisit with real usage data, not a STEP-2 decision made
   in a vacuum.

---

## 6. Design Decision: Partial-Success Model (formalized from `prd-v0.md` Section 4.3)

Already Gate-1-ratified; restated here as the binding spec (not re-litigated):
- One `POST /api/v1/links/batch` request always returns `200 OK` once it passes whole-request
  validation (Section 4) — individual item outcomes never change the top-level status.
- Every item is attempted independently; one item's failure has zero effect on any other item's
  processing, result, or persistence (Section 3.1's transactional-isolation consequence).
- No `"atomic": true`/all-or-nothing mode exists in this version — `prd-v0.md` Section 4.3 point 4
  explicitly declines to speculatively add one; nothing in FR-B1 asks for it.

---

## 7. Traceability Matrix

| FR / Risk | Endpoint / Mechanism | Section |
|---|---|---|
| FR-B1 | `POST /api/v1/links/batch` | 2, 3.1 |
| FR-B2 (order preservation) | `results[]` positional correlation | 3.1 |
| FR-B3 (vocabulary reuse) | Request/success/failure fields, error codes | 0, 1.1, 3.1 |
| FR-B4 (single-create unchanged) | N/A — no change to `POST /api/v1/links` | 2 (inventory unchanged) |
| R-BULK-1 (rate-limit amplification) | New batch-scoped Bucket4j limiter, 20 req/60s/IP | 5 |
| R-BULK-2 (self-invocation) | Binding constraint: call `LinkService` through the injected bean, never `this.createLink(...)` inside `LinkService.java` | 3.1 |
| R-BULK-3 (200 DB round-trips/request) | 100-item cap (unchanged from `prd-v0.md`) + rate limiter sized against this cost (Section 5, point 2) | 3.1, 5 |
| R-BULK-4 (per-item exception catching) | Binding constraint: all 7 `createLink()` exceptions caught in-loop, never propagated to `ApiExceptionHandler` | 1.1, 3.1 |
| R-BULK-5 (validation ordering) | Fixed check order: rate-limit -> parse -> batch-size -> per-item loop | 4 |
