# STEP-2 Specification: UX Flow (API Interaction Sequences) — url-shortener-bulk-shorten

**Phase:** STEP-2 · **Agent:** `ux-design` (skill: `skills/step2/ux-design/SKILL.md`)
**Stack:** java-spring · **Role:** services-mod · **Mode:** agentic · **Platform:** none
**Inputs read:** `step2/feature-spec.md` (this run's Part 1, above), `inputs/url-shortener-bulk-shorten/figma/`

**Figma check:** `mcp:figma` is declared optional in this agent's input contract. Per
`rules/mcp-convention.md`, this project's enabled MCP family set is code-graph only. Confirmed
directly:
```
$ find .claude/inputs/url-shortener-bulk-shorten/figma -type f
find: .claude/inputs/url-shortener-bulk-shorten/figma: No such file or directory
```
No Figma export exists for this feature — same as `url-shortener-core`'s ux-flow.md, "UX" here means
**API interaction sequences**, not UI screens: `java-spring` has no `ui` layer, and this feature adds
one endpoint, not a frontend.

---

## Sequence 1: Submit Batch -> Per-Item Results

This is the endpoint's core interaction: one client request carrying up to 100 items, processed
independently, producing one `results[]` entry per item. Drawn here with a **mixed outcome**
(some items succeed, some fail with different reasons) because that is the scenario that actually
exercises every rule in `feature-spec.md` Section 3.1/4 — an all-success batch is the same shape
with every `results[i].status` equal to `"CREATED"`, and is not drawn separately.

```
Batch Caller              Controller/BulkLinkService              LinkService (reused, unmodified)         Data Layer
     |                          |                                          |                                    |
     | 1. POST /api/v1/links/   |                                          |                                    |
     |    batch                |                                          |                                    |
     |    {items: [A, B, C]}   |                                          |                                    |
     |------------------------->|                                         |                                    |
     |                          | 2. rate-limit check (source IP only)    |                                    |
     |                          |    -- Bucket4j, new interceptor,        |                                    |
     |                          |       feature-spec.md Section 5         |                                    |
     |                          |    [bucket has tokens -- continue]      |                                    |
     |                          | 3. deserialize body                    |                                    |
     |                          | 4. items non-empty, items.length <= 100 |                                    |
     |                          |    -- feature-spec.md Section 4         |                                    |
     |                          |    [both pass -- enter per-item loop]   |                                    |
     |                          |                                          |                                    |
     |                          | 5. FOR item A (url ok, no customCode):  |                                    |
     |                          |    linkService.createLink(A.url, null)  |                                    |
     |                          |    -- called THROUGH the injected bean  |                                    |
     |                          |    reference, never self-invoked        |                                    |
     |                          |    (feature-spec.md 3.1 -- R-BULK-2)    |                                    |
     |                          |----------------------------------------->|                                   |
     |                          |                                          | 6. validate, generate code,      |
     |                          |                                          |    INSERT (own @Transactional     |
     |                          |                                          |    boundary, preserved because    |
     |                          |                                          |    the call went through the      |
     |                          |                                          |    proxy)                         |
     |                          |                                          |----------------------------------->|
     |                          |                                          |<---- row committed ----------------|
     |                          |<---- ShortLinkEntity(A) -----------------|                                    |
     |                          | 7. results[0] = {status: CREATED, ...}  |                                    |
     |                          |                                          |                                    |
     |                          | 8. FOR item B (url malformed):          |                                    |
     |                          |    linkService.createLink(B.url, null)  |                                    |
     |                          |----------------------------------------->|                                   |
     |                          |                                          | 9. validateUrl() throws          |
     |                          |                                          |    UrlMalformedException          |
     |                          |<---- exception -------------------------|                                    |
     |                          | 10. caught HERE, in the loop -- never   |                                    |
     |                          |     allowed to reach                    |                                    |
     |                          |     ApiExceptionHandler                 |                                    |
     |                          |     (feature-spec.md 1.1 -- R-BULK-4)   |                                    |
     |                          | 11. results[1] = {status: FAILED,       |                                    |
     |                          |     code: "URL_MALFORMED", ...}         |                                    |
     |                          |     -- item A's already-committed row   |                                    |
     |                          |     is completely unaffected            |                                    |
     |                          |                                          |                                    |
     |                          | 12. FOR item C (customCode taken):      |                                    |
     |                          |     linkService.createLink(C.url,       |                                    |
     |                          |     "alias1")                           |                                    |
     |                          |----------------------------------------->|                                   |
     |                          |                                          | 13. INSERT -- unique constraint  |
     |                          |                                          |     violation                     |
     |                          |                                          |----------------------------------->|
     |                          |                                          |<---- DataIntegrityViolation -------|
     |                          |                                          | 14. rethrown as                   |
     |                          |                                          |     CustomCodeTakenException       |
     |                          |<---- exception -------------------------|                                    |
     |                          | 15. caught HERE (same as step 10)       |                                    |
     |                          | 16. results[2] = {status: FAILED,       |                                    |
     |                          |     code: "CUSTOM_CODE_TAKEN", ...}     |                                    |
     |                          |                                          |                                    |
     | 17. 200 OK               |                                          |                                    |
     |     {results: [A, B, C], |                                          |                                    |
     |      successCount: 1,    |                                          |                                    |
     |      failureCount: 2}    |                                          |                                    |
     |<-------------------------|                                          |                                    |
```

**Notes on this sequence:**
- Steps 2-4 are the whole-request gate from `feature-spec.md` Section 4, drawn in the fixed order
  that section mandates: rate limit first (cheapest, no body parsed yet), then parse, then
  batch-size validation, then — and only then — the per-item loop. A request that fails any of
  steps 2-4 never reaches step 5 at all; see Sequence 2 for the rate-limit branch and the notes
  below for the batch-size branch.
- Steps 5-7, 8-11, and 12-16 are three independent iterations of the **same** per-item shape
  (call `createLink()` through the proxy -> succeed and record, or throw and get caught locally).
  A 100-item batch is this same three-step unit repeated up to 100 times, not a new pattern per
  item count.
- Step 6's parenthetical is the sequence's single most important annotation: `createLink()`'s
  `@Transactional` boundary is preserved *because* step 5 calls it through the injected
  `linkService` reference (proxy-intercepted), not as `this.createLink(...)` from a method added
  directly inside `LinkService.java` (which would silently bypass the proxy — `risk-register.md`
  R-BULK-2). This diagram deliberately shows `Controller/BulkLinkService` and `LinkService` as two
  separate swimlanes specifically to make that call boundary visible.
- Step 10/15's annotation is the diagram's second most important one: the exception thrown at step
  9/14 is caught **immediately by the caller of `createLink()`**, inside the loop — it never
  reaches `ApiExceptionHandler` (which would otherwise turn the *whole* request into a single
  400/409 response, discarding item A's already-committed success — `feasibility-report.md`
  Finding F1, `risk-register.md` R-BULK-4).
- Step 11's final clause ("item A's already-committed row is completely unaffected") is the
  visual trace of the transactional-isolation guarantee `feature-spec.md` Section 3.1 states in
  prose: item B's failure has no rollback or blocking effect on item A's already-committed
  transaction, because each item ran in its own independent `@Transactional` scope.
- **Batch-size/empty-batch branch (not separately diagrammed, described here):** if step 4 fails
  (`items` empty or `items.length > 100`), the response is `400 EMPTY_BATCH`/`BATCH_TOO_LARGE`
  immediately after step 4 — no item is ever attempted, so **zero** calls into `LinkService` and
  **zero** DB round-trips occur (`risk-register.md` R-BULK-5). This is a strict prefix of the
  sequence above (stops after step 4) rather than a shape different enough to warrant its own
  diagram.
- All-success case: identical diagram shape to the one above, with every iteration following the
  step-5-7 pattern (no iteration ever reaches step 8-16's catch branch), `successCount` equal to
  `items.length`, `failureCount: 0`.

---

## Sequence 2: Batch-Rate-Limited

A caller (or script) issuing batch requests faster than `feature-spec.md` Section 5's 20
requests/60-second ceiling, from one source IP.

```
Batch Caller              Service (new BatchRateLimitInterceptor, Bucket4j, keyed on source IP only)
     |                          |
     | Request 1..20            |
     | POST /api/v1/links/batch  (each request consumes 1 token, regardless of items.length)
     |------------------------->|
     |  200 OK (results[])       (bucket has tokens remaining each time -- request proceeds to
     |<-------------------------|   Sequence 1's steps 3 onward)
     |  X-RateLimit-Remaining: 19, 18, ... 0  (decrementing header on each response)
     |
     | Request 21                (bucket empty -- no tokens left in the 60s window)
     | POST /api/v1/links/batch
     |------------------------->|
     |                          | rate-limit check: 0 tokens available
     |                          | -- request is REJECTED before body deserialization,
     |                          |    before EMPTY_BATCH/BATCH_TOO_LARGE validation, and
     |                          |    before any item is attempted (feature-spec.md Section 4,
     |                          |    step 1 always runs first)
     | 429 Too Many Requests     |
     |    Retry-After: <n>       |
     |    X-RateLimit-Limit: 20  |
     |    X-RateLimit-Remaining: 0
     |    {code: "RATE_LIMITED"} |
     |<-------------------------|
     |
     | Request 22..N (within the same window)
     | POST /api/v1/links/batch
     |------------------------->|
     |  429 (same as above -- no 5xx, feature-spec.md Section 5 fail-safe requirement)
     |<-------------------------|
     |
     |  ... 60s window rolls forward, bucket refills ...
     |
     | Request N+1 (after window reset)
     | POST /api/v1/links/batch
     |------------------------->|
     |  200 OK                   (bucket has tokens again -- normal processing resumes,
     |<-------------------------|   Sequence 1's steps 3 onward)
```

**Notes on this sequence:**
- **This is a different bucket from `url-shortener-core`'s redirect limiter**, not a rescoped or
  reused instance of it. The redirect limiter (core `ux-flow.md` Sequence 2) is keyed on
  `(source IP, short code)` and governs `GET /{code}` only; this limiter is keyed on **source IP
  alone** (no code exists pre-creation for a batch of not-yet-created links) and governs
  `POST /api/v1/links/batch` only. A source IP that has exhausted the redirect limiter against one
  code is completely unaffected here, and vice versa — the two buckets share no state, no
  registration, and no threshold.
- The rate-limit check happens *before* body deserialization and *before* the `EMPTY_BATCH`/
  `BATCH_TOO_LARGE` check (`feature-spec.md` Section 4's step 1) — an over-limit request costs a
  bucket-map lookup only, never a JSON parse, never a DB round-trip, mirroring the "fail fast,
  cheap" precedent the redirect limiter already established (core `ux-flow.md` Sequence 2 notes).
- One token is consumed per **request**, not per item — a 20-request/60s ceiling caps aggregate
  item throughput at up to `20 x 100 = 2,000` creation attempts/minute/IP regardless of whether
  each request carries 1 item or 100 (`feature-spec.md` Section 5, justification point 1).
- This sequence applies only to `POST /api/v1/links/batch`. `POST /api/v1/links` (single-create)
  remains, as today, entirely unthrottled (FR-B4 — unchanged) — this feature does not add a
  limiter to the single-create path, only to the new batch path.
