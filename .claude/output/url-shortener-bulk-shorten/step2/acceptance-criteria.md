# STEP-2 Specification: Acceptance Criteria — url-shortener-bulk-shorten

**Phase:** STEP-2 · **Agent:** `acceptance-criteria` (skill: `skills/step2/acceptance-criteria/SKILL.md`)
**Stack:** java-spring · **Role:** services-mod · **Mode:** agentic · **Platform:** none
**Inputs read:** `step2/feature-spec.md` (Sections 1-7), `step2/ux-flow.md` (Sequences 1-2)
**Numbering:** continues from `url-shortener-core/step2/acceptance-criteria.md`, which ends at
AC25 — this document begins at **AC26**, per `rules/architecture.md` Write-Once Immutability
(the core feature's closed AC set is not renumbered or reopened).

## Method
Every AC below cites the `feature-spec.md` section and FR/risk id it verifies, per
`rules/testing.md` ("Every AC needs a test" — `evaluator` at STEP-5 cross-references these AC ids
against test names/tags). Coverage target, per the task brief: all-success, mixed partial-failure,
empty batch, over-limit batch, batch-level rate-limiting, and the two brownfield risk findings
(R-BULK-1 rate-limit amplification, R-BULK-2 self-invocation/transactional-boundary correctness).

---

## `POST /api/v1/links/batch` — Batch Create Short Links

### AC26 — Happy path: all items succeed
**GIVEN** a batch of 3 items, each a valid `url` with no `customCode` collisions
**WHEN** the caller `POST`s `{"items": [item1, item2, item3]}` to `/api/v1/links/batch`
**THEN** the response is `200 OK` with `results` containing exactly 3 entries, every entry
`status: "CREATED"` with a `shortCode`/`shortUrl`/`longUrl`/`createdAt`/`expiresAt` (same shape and
rules as the single-create endpoint's `201` body), `successCount: 3`, `failureCount: 0`
*(feature-spec.md 3.1; FR-B1, `ux-flow.md` Sequence 1 all-success note)*

### AC27 — Mixed outcome: partial success and multiple distinct failure reasons
**GIVEN** a batch of 3 items: item A valid, item B has a malformed `url`, item C has a `customCode`
that collides with an already-persisted link
**WHEN** the caller `POST`s `{"items": [A, B, C]}` to `/api/v1/links/batch`
**THEN** the response is `200 OK` (not 400/409/422) with `results[0].status: "CREATED"`,
`results[1] = {status: "FAILED", code: "URL_MALFORMED", ...}`,
`results[2] = {status: "FAILED", code: "CUSTOM_CODE_TAKEN", ...}`, `successCount: 1`,
`failureCount: 2`
*(feature-spec.md 3.1, Section 1.1's whole-request-vs-per-item distinction; `ux-flow.md` Sequence 1;
FR-B1, R-BULK-4)*

### AC28 — Empty batch rejected at the whole-request level
**GIVEN** a request body of `{"items": []}`
**WHEN** the caller `POST`s to `/api/v1/links/batch`
**THEN** the response is `400 Bad Request` with `code: "EMPTY_BATCH"`, and no item is processed (no
`results[]` array is produced, no DB row is written)
*(feature-spec.md 3.1, Section 4; FR-B1, prd-v0.md Section 4.4)*

### AC29 — Over-limit batch rejected at the whole-request level
**GIVEN** a request body with `items.length == 101`
**WHEN** the caller `POST`s to `/api/v1/links/batch`
**THEN** the response is `400 Bad Request` with `code: "BATCH_TOO_LARGE"`, and no item is processed
(zero DB writes, including for the first 100 otherwise-valid items)
*(feature-spec.md 3.1, Section 4; FR-B1, prd-v0.md Section 4.4)*

### AC30 — Boundary: exactly 100 items is accepted, not rejected
**GIVEN** a request body with `items.length == 100`, all items valid and non-colliding
**WHEN** the caller `POST`s to `/api/v1/links/batch`
**THEN** the response is `200 OK` with 100 `results[]` entries, all `status: "CREATED"`,
`successCount: 100` — the 100-item cap is inclusive, not exclusive
*(feature-spec.md 3.1; FR-B1, prd-v0.md Section 4.4 "exceeds the maximum of 100")*

### AC31 — Whole-request validation runs before any per-item work (zero-writes-on-reject)
**GIVEN** a request body with `items.length == 150`, where the first 100 items are individually
valid and would each succeed if processed
**WHEN** the caller `POST`s to `/api/v1/links/batch`
**THEN** the response is `400 BATCH_TOO_LARGE`, and **zero** rows are persisted for any of the 150
items — including the first 100, which are never attempted because the batch-size check runs before
the per-item loop starts
*(feature-spec.md Section 4; risk-register.md R-BULK-5, `ux-flow.md` Sequence 1's "batch-size/
empty-batch branch" note)*

### AC32 — Response order matches input order 1:1
**GIVEN** a batch of 4 items in the order `[fail-A, succeed-B, fail-C, succeed-D]` (alternating
outcomes, deliberately not grouped by outcome)
**WHEN** the caller `POST`s this batch to `/api/v1/links/batch`
**THEN** `results[0]` corresponds to `fail-A` (`status: "FAILED"`), `results[1]` to `succeed-B`
(`status: "CREATED"`), `results[2]` to `fail-C` (`status: "FAILED"`), `results[3]` to `succeed-D`
(`status: "CREATED"`) — positional correlation holds regardless of outcome interleaving
*(feature-spec.md 3.1; FR-B2)*

### AC33 — `CODE_SPACE_EXHAUSTED` surfaces per-item, never as a whole-batch failure
**GIVEN** a batch of 2 items: item A has no `customCode` and the server-generated-code retry budget
is exhausted for item A specifically (pathological, per core spec Section 3.1); item B is otherwise
valid and non-colliding
**WHEN** the caller `POST`s `{"items": [A, B]}` to `/api/v1/links/batch`
**THEN** the response is `200 OK` (not a whole-request `503`) with `results[0] = {status: "FAILED",
code: "CODE_SPACE_EXHAUSTED", ...}` and `results[1].status: "CREATED"` — item B's success is
unaffected by item A's pathological failure
*(feature-spec.md Section 1.1, 3.1; feasibility-report.md Finding F1, risk-register.md R-BULK-4)*

---

## Transactional Isolation (R-BULK-2 regression guard)

### AC34 — A failing item does not affect any other item's persistence
**GIVEN** a batch of 3 items `[succeed-A, fail-B (malformed url), succeed-C]`
**WHEN** the caller `POST`s this batch to `/api/v1/links/batch`
**THEN** exactly 2 rows are persisted (for A and C), item B's failure causes no rollback of A's or
C's already-committed/subsequently-committed rows, and re-querying `GET /api/v1/links/{A.shortCode}
/stats` and `GET /api/v1/links/{C.shortCode}/stats` immediately after the batch response both
return `200 OK` — proving A and C are durably persisted independent of B's outcome
*(feature-spec.md 3.1 "Transactional isolation consequence"; risk-register.md R-BULK-2 — this AC is
the regression guard that would catch a broken transactional boundary if a future change
reintroduced self-invocation inside `LinkService.java`, per the task's explicit requirement that an
AC exist for this even though R-BULK-2 is not independently testable as its own isolated behavior)*

### AC35 — A later item's failure does not roll back an earlier item's success (ordering-sensitive variant of AC34)
**GIVEN** a batch of 2 items `[succeed-A, fail-B (custom code collision)]`, submitted in that exact
order
**WHEN** the caller `POST`s this batch to `/api/v1/links/batch`
**THEN** the response's `results[0].status: "CREATED"` for A **and** a direct lookup of A's
`shortCode` via `GET /{A.shortCode}` (redirect) succeeds with `302 Found` immediately after the
batch response returns — item A's commit is not held pending or reverted by item B's later failure
within the same request
*(feature-spec.md 3.1; risk-register.md R-BULK-2 — complements AC34 by testing the specific failure
mode self-invocation would cause: a later item's uncommitted/rolled-back transaction silently
dragging an earlier item's committed one down with it)*

---

## Batch-Scoped Rate Limiting (R-BULK-1 mitigation)

### AC36 — Within the batch rate limit
**GIVEN** a source IP has made fewer than 20 `POST /api/v1/links/batch` requests in the trailing
60-second window
**WHEN** that IP sends another `POST /api/v1/links/batch` request (with a valid, non-empty,
<=100-item body)
**THEN** the response is `200 OK` (not `429`), with response headers `X-RateLimit-Limit: 20` and
`X-RateLimit-Remaining` decremented by 1 from the prior value
*(feature-spec.md Section 5; FR-B1, R-BULK-1 mitigation)*

### AC37 — Batch rate limit exceeded
**GIVEN** a source IP has already made 20 `POST /api/v1/links/batch` requests within the trailing
60-second window
**WHEN** that IP sends a 21st `POST /api/v1/links/batch` request within that same window
**THEN** the response is `429 Too Many Requests` with `code: "RATE_LIMITED"`, a `Retry-After`
header, and **no** item from the 21st request's body is processed — zero `results[]` entries, zero
DB writes, even if the body would otherwise have been entirely valid
*(feature-spec.md Section 5, Section 4; risk-register.md R-BULK-1, `ux-flow.md` Sequence 2)*

### AC38 — Batch rate limiter is scoped independently from the redirect limiter
**GIVEN** source IP `X` has exhausted its rate limit against `POST /api/v1/links/batch` (per AC37)
**WHEN** the same IP `X` sends `GET /{code}` for any existing, non-expired code
**THEN** the response is `302 Found` — the redirect endpoint's `(IP, code)`-keyed limiter
(`url-shortener-core` feature-spec.md Section 6) is entirely unaffected by the batch limiter's state,
and conversely, an IP that has exhausted the redirect limiter against a code is unaffected here (a
fresh batch request from that same IP still consumes from its own, independent bucket)
*(feature-spec.md Section 5 "different bucket... not a rescoped or reused instance"; risk-register.md
R-BULK-1's explicit note that this is a new rule, not a reopening of FR-9)*

### AC39 — Rate limit is per source IP, not per batch size
**GIVEN** source IP `X` has made 19 `POST /api/v1/links/batch` requests in the trailing 60-second
window, the most recent carrying 100 items and the next carrying only 1 item
**WHEN** IP `X` sends its 20th request (1 item) followed immediately by its 21st request (1 item)
**THEN** the 20th request succeeds (`200 OK`) and the 21st is `429 RATE_LIMITED` — the limiter
counts **requests**, not items, so a request's `items.length` has no bearing on how many tokens it
consumes
*(feature-spec.md Section 5 "one token is consumed per request, not per item"; R-BULK-1)*

---

## Regression: Existing Single-Create Endpoint Unaffected (FR-B4)

### AC40 — `POST /api/v1/links` behavior is unchanged by this feature
**GIVEN** the `url-shortener-bulk-shorten` feature has been deployed (batch endpoint exists)
**WHEN** the caller `POST`s a single valid `{"url": "https://example.com"}` to the existing
`/api/v1/links` endpoint
**THEN** the response is `201 Created` with the exact same body shape, status code, and absence of
rate limiting as before this feature existed (`url-shortener-core/step2/acceptance-criteria.md`
AC01 still holds, byte-for-byte, against the post-feature codebase)
*(feature-spec.md Section 2 "endpoint inventory... unchanged"; FR-B4, impact-analysis.md "Does the
existing single-create endpoint's behavior change? No.")*

---

## Coverage Summary

| Scenario | AC(s) |
|---|---|
| All-success batch | AC26 |
| Mixed partial-failure batch | AC27, AC33 |
| Empty batch | AC28 |
| Over-limit batch | AC29, AC31 |
| Boundary (exactly 100) | AC30 |
| Order preservation | AC32 |
| Transactional isolation (R-BULK-2 regression guard) | AC34, AC35 |
| Batch-level rate limiting (R-BULK-1 mitigation) | AC36, AC37, AC38, AC39 |
| Existing single-create endpoint regression | AC40 |

**Total: 15 new acceptance criteria (AC26-AC40),** continuing `url-shortener-core`'s AC01-AC25
without renumbering or reopening it. Every category the task brief named is covered — all-success,
mixed partial-failure, empty batch, over-limit batch, batch-level rate-limiting — plus both
brownfield risk findings: R-BULK-1 directly (AC36-AC39) and R-BULK-2 via a positive regression guard
(AC34, AC35) that would fail if per-item transactional isolation were ever broken by a future
self-invocation regression, even though R-BULK-2 itself (a Spring AOP implementation-mechanism risk)
is not independently AC-testable as its own isolated behavior.
