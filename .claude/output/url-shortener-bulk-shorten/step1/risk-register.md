# STEP-1: Risk Register — url-shortener-bulk-shorten

**Agent:** risk-analysis · **Input:** `step1/feasibility-report.md`, `step1/impact-analysis.md`,
real source (`RateLimitInterceptor.java`, `WebConfig.java`, `RateLimiterService.java`,
`LinkService.java`, `feature-spec.md` Section 6, all re-read directly for this artifact).

## Overall Risk Level: **MEDIUM** (one HIGH-leaning item — R-BULK-1 — driven by a genuine
amplification of an existing, deliberately-scoped gap; no BLOCKER-level item; buildable as-is
per `feasibility-report.md`).

---

## R-BULK-1 — Batch endpoint has zero rate limiting, amplifying an existing gap by up to 100x

**Severity: HIGH (recommend addressing before/at STEP-3, not silently shipping)**

**Evidence (verbatim from real code):**
- `WebConfig.java:24-26`:
  ```java
  registry.addInterceptor(rateLimitInterceptor)
          .addPathPatterns("/*")
          .excludePathPatterns("/api/**", "/actuator/**");
  ```
  `"/*"` matches exactly one path segment (i.e. `GET /{code}` only); `/api/**` is explicitly
  excluded. This is confirmed intentional, not an oversight: `feature-spec.md` Section 6 states
  "Scope | `GET /{code}` (redirect) only — not create, not stats (FR-9's text names only 'the
  redirect endpoint')". `RateLimiterService.tryConsume(sourceIp, code)` is keyed on the *resolved*
  short code (`RateLimitInterceptor.java:44`), which structurally does not exist yet at
  create-time — the limiter cannot apply to a create-time endpoint without new code regardless of
  routing.
- Consequence: today, `POST /api/v1/links` (single create) already has **no** rate limit — this is
  an existing, deliberate, Gate-approved design decision, not a defect this feature introduces.
- **What changes with this feature:** the new `POST /api/v1/links/batch` endpoint inherits the
  same `/api/**` exclusion (confirmed: it is a new path under `/api/v1/`, so `WebConfig.java`'s
  existing `excludePathPatterns("/api/**", ...)` covers it with zero code change — see
  `impact-analysis.md`). But **one HTTP request to the batch endpoint now performs up to 100
  link-creation operations** (the proposed cap, `prd-v0.md` Section 4.4), where one request to the
  single-create endpoint performs exactly 1. An attacker (or a misbehaving client) issuing the same
  request rate against `/batch` instead of the single-create endpoint gets up to a **100x
  amplification** in short-link creation throughput — filling the `short_link` table, and, more
  acutely, filling `RESERVED_CODES`-adjacent code space or driving DB load — for the same number of
  HTTP requests and the same absence of any limiter in front of either.

**This is a new risk, not merely an old one restated** — the *existence* of an unthrottled
create endpoint was already an accepted, scoped decision (FR-9's text names only the redirect
endpoint); what's new is that this feature multiplies its worst-case throughput by up to 100x per
request, which the original FR-9 scoping decision was never asked to weigh.

**Recommendation (for Gate 2/Gate 3, not decided unilaterally here):** add a **new, narrow,
request-level** limiter in front of `POST /api/v1/links/batch` specifically (e.g. N batch-requests
per source-IP per rolling window, using **Bucket4j**, already a dependency —
`dependency-audit.md` confirms zero new library needed). This is additive protection for a newly
introduced amplification surface; it does **not** require reopening or amending `feature-spec.md`
Section 6's FR-9 scoping decision (which governs the redirect path's *existing* limiter, a
different mechanism/key/purpose) — it would be a new rule, not a change to the old one, so it does
not trigger `rules/architecture.md` Write-Once Immutability's contract-delta-amendment path for the
already-closed `url-shortener-core` feature. Flagged here as a STEP-1 discovery item per this run's
task scope; **not implemented at STEP-1** (STEP-1 is discovery-only).

**Secondary mitigation already in the proposed design:** the 100-item batch cap
(`prd-v0.md` Section 4.4, `EMPTY_BATCH`/`BATCH_TOO_LARGE`) bounds the *per-request* amplification
factor at 100x, not unbounded — it is a real, load-bearing mitigation for this exact risk, just not
a complete one (100x is still a large multiplier against zero baseline throttling).

---

## R-BULK-2 — Self-invocation would silently break per-item `@Transactional` semantics

**Severity: MEDIUM (design-correctness risk, not a runtime crash — degrades silently)**

**Evidence:** `LinkService.java:67`, `@Transactional public ShortLinkEntity createLink(...)`.
Spring's `@Transactional` is implemented via a runtime AOP proxy wrapping the bean; the proxy only
intercepts calls that arrive **through the bean reference** (e.g. another bean calling
`linkService.createLink(...)`). A call made **from within `LinkService` itself** — e.g.
`this.createLink(...)` inside a new bulk method added directly to `LinkService.java` — bypasses the
proxy entirely (this is documented, well-known Spring AOP self-invocation behavior, not
speculative). If STEP-4's generator takes the "add a method inside `LinkService.java`" branch of
`prd-v0.md`'s "(or a thin new service class delegating to createLink per item)" phrasing, each
item's transaction boundary would silently stop being independently committed/rolled back per
Spring's normal contract — the code would still compile and likely still "work" in casual testing
(H2 autocommit-adjacent behavior can mask this), making it a risk that surfaces later, not at
build-verdict time.

**Mitigation (already adopted in `impact-analysis.md`'s recommended design):** implement the bulk
loop in a **new, separate service class** (or the controller) that holds `LinkService` as an
injected dependency and calls `linkService.createLink(...)` through the proxy — preserving today's
per-item transactional behavior exactly, with no new transactional code to write or test.
**Action:** STEP-3 Technical Design should encode this as an explicit constraint on the generator,
not leave it to implementation-time judgment.

---

## R-BULK-3 — Up to 200 sequential synchronous DB round-trips per single HTTP request

**Severity: MEDIUM (performance/availability, not correctness)**

**Evidence:** `LinkService.createWithGeneratedCode()` (`LinkService.java:88-114`) performs **two**
`saveAndFlush` calls per generated-code item (insert placeholder, derive code, update) —
`createWithCustomCode()` performs one. A worst-case 100-item batch where every item omits
`customCode` therefore drives **up to 200 sequential, synchronous DB round-trips within one HTTP
request-handling thread** before any response is returned to the client. `prd-v0.md` Section 4.4
already identifies the absence of async infrastructure in this codebase (confirmed independently:
no task executor, no queue, grepped exhaustively) as the reason the endpoint must be synchronous at
all — this risk is that consequence's concrete cost: one client can tie up one Tomcat worker thread
for the duration of up to 200 DB writes, not 1-2 as with the single-create endpoint. Combined with
R-BULK-1 (no throttle on request *rate*), a burst of concurrent batch requests could exhaust the
web server's worker thread pool considerably faster than equivalent single-create traffic would,
degrading availability for the redirect path too (same process, same thread pool).

**Recommendation:** the 100-item cap (already proposed) is the primary bound; flagged here so
Gate 2/3 sizes the cap and any future load testing with this specific multiplier (up to 2x DB calls
per item, up to 100 items) in mind, not just "100 requests." No code change recommended at STEP-1 —
discovery-only finding.

---

## R-BULK-4 — Partial-success correctness depends on catching exceptions per-item, not per-request

**Severity: MEDIUM (correctness — a naive implementation silently reverts to all-or-nothing)**

**Evidence:** `ApiExceptionHandler.java` is a `@RestControllerAdvice` — it intercepts exceptions at
the *request* level, producing exactly one HTTP response per uncaught exception. All six of
`createLink()`'s possible exceptions (`InvalidUrlException`, `InvalidCustomCodeShapeException`,
`ReservedCodeException`, `CustomCodeTakenException`, `CodeSpaceExhaustedException`, and
transitively `LinkUnavailableException` though not reachable from create) would, if left uncaught
inside the new batch loop, propagate to `ApiExceptionHandler` and turn **one bad item** into a
single whole-request 400/409/503 response — silently collapsing the intended partial-success model
(`prd-v0.md` Section 4.3) into accidental all-or-nothing on the *first* failing item, with the
remaining items in the batch never even attempted. This is a straightforward, well-understood
implementation requirement (catch each of the six exception types inside the per-item loop, map
each to a `BatchItemResult`, never let them escape the loop) — not a novel risk — but is flagged
explicitly because getting it wrong produces no compile error and no obviously-wrong test result
under a happy-path-only test (it only shows up under a mixed-outcome batch, which
`rules/testing.md`'s Negative & Edge Cases requirement should specifically cover at STEP-5).

**Recommendation:** STEP-2 acceptance criteria and STEP-5 test-generation should include an
explicit mixed-outcome batch test (some items succeed, some fail with different error codes) as a
required AC/test — flagged forward for those phases.

---

## R-BULK-5 — Batch-level validation ordering (empty/oversized) must precede any per-item work

**Severity: LOW**

**Evidence:** `prd-v0.md` Section 4.4 proposes `EMPTY_BATCH`/`BATCH_TOO_LARGE` as batch-level
(request-level) 400s, checked "before processing any item." This is straightforward to implement
correctly (a length check before the loop starts) but is called out because getting the order
wrong (e.g., validating batch size only after starting to process items) would waste DB work on a
request that's going to be rejected anyway, and — combined with R-BULK-1's absence of a request
throttle — makes a malformed/oversized-batch flood cheaper to send than to reject if checked late.

**Recommendation:** trivial to get right; flagged for STEP-4 generator/STEP-5 test coverage
(a test asserting zero DB writes occur for a batch that fails `BATCH_TOO_LARGE`/`EMPTY_BATCH`).

---

## Risks explicitly considered and ruled out (not included above)

- **New data-layer schema risk** — ruled out; `impact-analysis.md` and `feasibility-report.md` both
  independently confirm no schema change occurs, so no migration-safety risk
  (`rules/data-layer.md`) applies.
- **New dependency / CVE surface** — ruled out; `dependency-audit.md` confirms zero new
  dependencies, so no new entries for `security-audit`'s (STEP-6.2) CVE check.
- **Breaking change to the existing single-create endpoint** — ruled out; `impact-analysis.md`
  confirms `LinkController`'s existing method and `LinkService.createLink()`'s body are both
  unedited under the recommended design.

## Summary Table

| ID | Risk | Severity | Recommended disposition |
|---|---|---|---|
| R-BULK-1 | Batch endpoint has no rate limit — up to 100x amplification of an already-unthrottled create path | HIGH | Add a new, narrow, request-level Bucket4j limiter scoped to `/api/v1/links/batch`; decide at Gate 2/3 |
| R-BULK-2 | Self-invocation inside `LinkService.java` would silently break per-item `@Transactional` | MEDIUM | Implement bulk loop in a new service class / controller, not inside `LinkService.java` |
| R-BULK-3 | Up to 200 sequential synchronous DB round-trips per request, one Tomcat thread held throughout | MEDIUM | Keep the 100-item cap; size load tests accordingly; no code change at STEP-1 |
| R-BULK-4 | Per-item exceptions must be caught in-loop or partial-success silently degrades to all-or-nothing | MEDIUM | Explicit mixed-outcome AC/test required at STEP-2/STEP-5 |
| R-BULK-5 | Batch-level size/empty validation must run before any per-item DB work | LOW | Order check before loop; test zero-writes-on-reject |
