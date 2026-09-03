# PRE-WORK: url-shortener-bulk-shorten (brownfield enhancement)

**Stack:** java-spring · **Role (filed):** services-mod (posture=mod, layers_in_scope=[api, service])
**Mode:** agentic · **Platform:** none
**Raw input:** `.claude/inputs/url-shortener-bulk-shorten/supporting-docs/request.md`
**Agents combined in this artifact:** `triage`, `architecture-analysis`, `codebase-context`,
`requirement-ingestion`, `posture-feasibility`, `role-feasibility-pass1` (per `rules/output-path.md`,
prework writes to a single `prework/prd-v0.md`; each section below is one agent's contribution).

---

## 1. Triage Verdict

| Field | Value | Evidence |
|---|---|---|
| `feature_shape` | **enhancement** | Raw request adds a new capability ("submit multiple URLs... get back multiple short links") to an already-shipped, already-COMPLETE service (`url-shortener-core`, `_run-log.md` shows STEP-0→COMPLETE on commit `efe8899`). Not greenfield (real code exists), not incident-fix (nothing is broken), not refactor (no reshaping of existing behavior — existing single-create endpoint is untouched). |
| `recommended_role` | **services-mod fits, confirmed** | See Section 6 (role-feasibility-pass1) for the full check. Short version: the enhancement needs one new `api`-layer endpoint and one new (or extended) `service`-layer method that calls the existing, unmodified `LinkService.createLink(url, customCode)` per item — no `data`-layer schema change (Section 2's `ShortLinkEntity` already stores one link per row; a batch of N links is just N of the existing rows, not a new table/shape). `layers_in_scope: [api, service]` is sufficient. |
| `retry_budget` | **5** | Checked `.claude/run-history/_online-learning.yaml` verbatim (below) — it has exactly one matrix row, `java-spring/greenfield`, with `success_rate: 0.875` and `retries_consumed_total: 1`. There is **no** `java-spring/services-mod` row. Per `rules/retry-policy.md`, `adaptive-gate` may calibrate 3-8 only "based on historical pass rate for the stack+role combination" — with zero history for this exact matrix row, there is nothing to calibrate against, so the framework default of 5 applies uncalibrated. This matches what `_role-context.yaml` (already resolved for this feature) records: `retry_budget: 5  # adaptive-gate: java-spring/services-mod has no history yet (different matrix row than greenfield)`. |

**`_online-learning.yaml` verbatim (only row present):**
```yaml
matrix_rows:
  java-spring/greenfield:
    gate_decisions:
      gate_0: [APPROVED]
      gate_1: [GO]
      gate_2: [APPROVED]
      gate_3: [APPROVED]
      gate_6: [NO_EXCLUSIONS]
    pass_history: [PASS]
    reliability: {success_rate: 0.875, mttr_seconds: null, retries_consumed_total: 1}
```

---

## 2. Architecture Analysis (real codebase, read verbatim)

### 2.1 Real current API surface
Grepped `@PostMapping|@GetMapping|@PutMapping|@DeleteMapping|@PatchMapping|@RequestMapping` across
`service-java-spring/src/main/java/com/aisdlc/urlshortener/`. Exactly three endpoints exist, all in
`api/LinkController.java`:

```
LinkController.java:36:    @PostMapping("/api/v1/links")
LinkController.java:52:    @GetMapping("/{code}")
LinkController.java:62:    @GetMapping("/api/v1/links/{code}/stats")
```

No bulk/batch endpoint exists today. No other `@RestController` exists in the package.

### 2.2 Real current field names (verbatim from source — this is the part that matters most for
designing a consistent bulk endpoint)

`api/dto/CreateLinkRequest.java`:
```java
public record CreateLinkRequest(String url, String customCode) {}
```

`api/dto/LinkResponse.java`:
```java
public record LinkResponse(String shortCode, String shortUrl, String longUrl, Instant createdAt, Instant expiresAt) {}
```

`api/dto/ErrorResponse.java`:
```java
public record ErrorResponse(Instant timestamp, int status, String error, String code, String message) {}
```

`api/dto/StatsResponse.java` (for reference — not touched by this feature):
```java
public record StatsResponse(String shortCode, String longUrl, Instant createdAt, Instant expiresAt,
    long totalClicks, List<ClicksByDay> clicksByDay, List<ClicksByCountry> clicksByCountry) {}
```

**Important note found in `CreateLinkRequest.java`'s own Javadoc**, confirming the task brief's premise:
> "Field names match feature-spec.md Section 1 (`url`, `customCode`) — the authoritative wire
> contract... See generator-summary.md for the discrepancy with the terser step3/api-contract.yaml
> stub, which uses different field names (`targetUrl`, `code`)."

I independently confirmed `step3/api-contract.yaml` carries its own header explaining it was
**corrected post-hoc** to match `feature-spec.md`/the actual code — its own first line reads
`"CORRECTED post-STEP-4 (contract-delta amendment...) — the conductor's original draft of this file
used different field/error-code names than the Gate-2-approved step2/feature-spec.md."` So
`feature-spec.md`'s vocabulary (`url`, `customCode`, `shortCode`, `shortUrl`, `longUrl`, `createdAt`,
`expiresAt`, and the `code` error-vocabulary tokens below) is ground truth, exactly as the task brief
states, and is what this PRD designs the bulk endpoint's field names against.

**Real error-`code` vocabulary in use** (grepped `ApiExceptionHandler.java` + exception classes):
`INVALID_URL_SCHEME`, `URL_TOO_LONG`, `URL_MALFORMED`, `INVALID_CUSTOM_CODE_SHAPE`, `RESERVED_CODE`,
`CUSTOM_CODE_TAKEN`, `CODE_NOT_FOUND`, `CODE_SPACE_EXHAUSTED`, `RATE_LIMITED` (thrown by
`RateLimitInterceptor`, not in the exception-handler grep but confirmed in `feature-spec.md` Section
6 and `WebConfig.java`), plus two catch-all codes `MALFORMED_REQUEST` (400, unreadable body) and
`INTERNAL_ERROR` (500, unhandled `Exception`).

### 2.3 Real layer boundaries — confirmed to actually hold
Per `rules/architecture.md` Dependency Direction (`api -> service -> data`, no layer imports from
above), grepped for violating imports directly in the real source:

```
grep '^import com\.aisdlc\.urlshortener\.api' service/**/*.java   -> No matches found
grep '^import com\.aisdlc\.urlshortener\.(api|service)' data/**/*.java -> No matches found
```

Confirmed clean: `service/` imports nothing from `api/`; `data/` imports nothing from `service/` or
`api/`. `ApiExceptionHandler.java`'s own Javadoc states this explicitly and correctly: "Lives in the
api layer, not service — service exceptions carry no HTTP concerns of their own." `ShortLinkEntity`'s
Javadoc likewise: "Data layer: no HTTP/api-layer imports here." Both are true as verified above, not
just asserted in comments.

### 2.4 Relevant supporting real-code facts for the bulk design
- `LinkService.createLink(String url, String customCode)` (service/LinkService.java:68) is
  `@Transactional`, validates the URL, and dispatches to either `createWithCustomCode` (insert, catch
  `DataIntegrityViolationException` → `CustomCodeTakenException`) or `createWithGeneratedCode`
  (Candidate A insert-then-derive-then-update, one transaction per call). Each call is a **complete,
  independent, self-contained unit of work** — there is no existing multi-row transactional grouping
  anywhere in this service today.
- `ShortLinkEntity` (data/ShortLinkEntity.java) maps 1:1 to the `short_link` table, one row per link,
  no batch/group concept in the schema. A batch of N submitted URLs is structurally just N ordinary
  rows — **no new table, no new column, no `data`-layer change is needed** to support bulk creation.
- `WebConfig.java` registers `RateLimitInterceptor` only on `/*` (single root-level segment) excluding
  `/api/**` — i.e., the existing rate limiter **does not and cannot** apply to any `/api/v1/...`
  endpoint (create/stats), only to the redirect endpoint. A bulk-create endpoint under `/api/v1/`
  inherits this same exclusion by construction; no separate rate-limit change is implied for the api
  layer create path itself, consistent with `feature-spec.md` Section 6's explicit scoping ("create,
  not stats" are not the abuse surface FR-9 named).
- `LinkController` is a single `@RestController` with a constructor-injected `LinkService` — no
  existing "batch" or "bulk" abstraction anywhere in `api/` or `service/` to reuse or diverge from.

---

## 3. Codebase Context (in-scope files, layer map, drift)

### 3.1 In-scope files for `layers_in_scope: [api, service]`
```
api/
  LinkController.java            <- new endpoint goes here
  ApiExceptionHandler.java       <- new error-code mappings (if any) go here
  RateLimitInterceptor.java      <- unaffected (excluded from /api/**, confirmed 2.4)
  WebConfig.java                 <- unaffected, no change needed
  util/ClientIpResolver.java     <- unaffected (redirect-path only)
  dto/CreateLinkRequest.java     <- pattern to extend/wrap for a batch request DTO
  dto/LinkResponse.java          <- pattern to extend/wrap for a per-item batch result
  dto/ErrorResponse.java         <- pattern for per-item error shape inside a batch result
  dto/StatsResponse.java         <- unaffected
service/
  LinkService.java                <- add a bulk-oriented method here (or a thin new
                                      service class delegating to createLink per item)
  CodeGenerator.java               <- unaffected, reused as-is per item
  GeoLookupService.java            <- unaffected (redirect-path only)
  RateLimiterService.java          <- unaffected (redirect-path only)
  exception/*.java (6 files)       <- reused as-is per item; possibly one new exception
                                      type for batch-level validation (empty/oversized batch)
```
`data/` is **out of scope** for this role and, per Section 2.4, is not needed by this feature either
— no drift between what the role permits and what the feature requires.

### 3.2 Drift vs. standards
None found. The existing code already follows `stacks/java-spring/standards/naming.md` conventions
(camelCase JSON fields, matching `rules/coding-standards.md`), has no dead code, no silent catches
(every `catch (DataIntegrityViolationException)` in `LinkService.java` rethrows a specific,
documented exception type), and no `System.out`/`print` usage (SLF4J `Logger` used in
`ApiExceptionHandler` and, presumably, elsewhere per the stack standard). Nothing here blocks or
complicates the bulk-shorten enhancement.

---

## 4. Requirement Ingestion — Normalized PRD v0

### 4.1 Source
`.claude/inputs/url-shortener-bulk-shorten/supporting-docs/request.md` (verbatim, full text):
> "Right now creating short links is one-at-a-time. We have a use case where someone wants to
> shorten a whole list of URLs in one go instead of calling the API N times. Add a way to submit
> multiple URLs and get back multiple short links.
>
> Didn't think through what should happen if one of the URLs in the batch is bad (invalid, or the
> alias is already taken) — should the whole batch fail, or just that one item? Also haven't picked
> a limit on how many URLs can go in one batch. Use your judgment and flag it if it matters."

No jira export, no other supporting docs present in `inputs/url-shortener-bulk-shorten/` (only
`supporting-docs/request.md` exists — confirmed by directory listing).

### 4.2 Functional Requirement (new)
**FR-B1 (Must):** A client can submit a batch of URLs (each optionally paired with a `customCode`,
exactly the same two fields the existing single-create endpoint accepts) in one HTTP request, and
receive back, in one response, a per-item result — either a created short link or a per-item failure
reason — without needing to issue N separate `POST /api/v1/links` calls.

### 4.3 Open Question 1 — Partial-Failure Model (explicitly deferred by the requester; resolved here)

**Proposed model: per-item independent processing ("partial success"), not all-or-nothing.**

Endpoint: `POST /api/v1/links/batch`

Request:
```json
{
  "items": [
    { "url": "https://example.com/a", "customCode": "alias1" },
    { "url": "not-a-url" },
    { "url": "https://example.com/c", "customCode": "alias1" }
  ]
}
```
(`items[].url` / `items[].customCode` — identical field names to the existing single-item
`CreateLinkRequest(url, customCode)`, Section 2.2, so a client already integrated with the single
endpoint needs zero field-name relearning.)

Response — **`200 OK`** always (the batch request itself succeeded; individual item outcomes are
carried in the body, not the top-level HTTP status) — one result per input item, **same order**:
```json
{
  "results": [
    { "status": "CREATED", "shortCode": "abc1234", "shortUrl": "https://<host>/abc1234",
      "longUrl": "https://example.com/a", "createdAt": "...", "expiresAt": "..." },
    { "status": "FAILED", "code": "URL_MALFORMED", "message": "url is not a well-formed URI" },
    { "status": "FAILED", "code": "CUSTOM_CODE_TAKEN", "message": "Custom code already in use: alias1" }
  ],
  "successCount": 1,
  "failureCount": 2
}
```
Per-item success fields reuse `LinkResponse`'s exact field names (`shortCode`, `shortUrl`, `longUrl`,
`createdAt`, `expiresAt`); per-item failure fields reuse `ErrorResponse`'s `code`/`message` vocabulary
(Section 2.2) verbatim — so every `code` value an item can fail with is one already defined by the
single-create endpoint (`INVALID_URL_SCHEME`, `URL_TOO_LONG`, `URL_MALFORMED`,
`INVALID_CUSTOM_CODE_SHAPE`, `RESERVED_CODE`, `CUSTOM_CODE_TAKEN`) — no new per-item error vocabulary
needs inventing.

**Justification for partial-success over all-or-nothing:**
1. **Matches the existing transactional shape of the code being reused, not fought against.**
   `LinkService.createLink()` (Section 2.4) is already `@Transactional` **per call**, and is the one
   piece of logic this feature must reuse rather than duplicate (`rules/coding-standards.md` No Dead
   Code / code-reuse expectation). Wrapping N calls to it in one outer transaction to get
   all-or-nothing semantics would require either (a) a new, wider transaction boundary that fights
   Spring's self-invocation proxy semantics if called from within the same class, or (b) accepting
   that a partial batch already committed some rows via `saveAndFlush` (used internally, Section 2.4)
   before a later item fails — meaning true all-or-nothing isn't even achievable cheaply against the
   current implementation without a much larger rewrite of `createLink()` itself, which is out of
   scope for a services-mod enhancement that should not touch already-tested, already-shipped
   single-create behavior.
2. **All-or-nothing is worse UX for the stated use case.** The requester's own scenario is "shorten a
   whole list of URLs in one go" — e.g. importing 200 links, one of which happens to have a typo'd
   scheme. All-or-nothing means one bad row (or one alias collision, which is exactly the
   first-come-first-served behavior the single-create endpoint already treats as an expected,
   non-exceptional outcome — `CUSTOM_CODE_TAKEN` is a normal 409, not a system fault) discards 199
   good ones, forcing the caller to bisect the batch to find the bad item. Partial-success returns
   exactly that diagnosis in one round-trip.
3. **Consistent with existing precedent elsewhere in this codebase for exactly this trade-off.**
   `technical-design.md`'s Geo Lookup section documents a deliberate "fail-soft... consistent with
   the click-write-isolation pattern this project has used before for non-critical side effects" —
   i.e., this codebase already has a stated preference for isolating one failure from sinking an
   otherwise-successful operation. Per-item isolation in a batch is the same principle applied to a
   new surface, not a new philosophy.
4. **A future strict/atomic mode is not precluded.** Nothing here prevents adding an optional
   `"atomic": true` request flag later if a real use case demands it — but nothing in the raw request
   asks for atomicity, and speculative support for an unrequested mode would violate the "no
   unrequested scope" instinct as well as add complexity with no current consumer (echoing
   `feature-spec.md` Section 4's own reasoning for not adding an unused status-code branch).

### 4.4 Open Question 2 — Batch-Size Cap (explicitly deferred by the requester; resolved here)

**Proposed cap: 100 items per batch request.** New validation rule, checked before processing any
item:
- `items` empty or absent → `400`, new code `EMPTY_BATCH` ("items must contain at least one URL").
- `items.length > 100` → `400`, new code `BATCH_TOO_LARGE` ("batch exceeds the maximum of 100
  items").

**Justification:**
1. **No background job/queue infrastructure exists in this codebase to fall back on.** Grepped the
   full `service-java-spring` tree (Section 2.1's file listing is exhaustive) — there is no async
   task executor, no message queue, no polling/status-check endpoint pattern anywhere. A bulk
   endpoint here is necessarily synchronous request/response, processing every item in the same HTTP
   request-handling thread before returning. An unbounded (or very large) cap turns one client
   request into an unbounded amount of synchronous DB work (each item does at least one
   `saveAndFlush`, Section 2.4, i.e. a real round-trip, not a buffered batch insert — the existing
   Candidate A code path is explicitly one-row-at-a-time by design) on a service that also has to
   stay responsive for the redirect path's own load. A firm cap bounds worst-case per-request latency
   and DB load to something predictable.
2. **100 keeps the request body itself bounded and sane.** Each `url` can be up to 2048 chars
   (`feature-spec.md` 3.1); 100 items is a ~200KB worst-case JSON body — comfortable for a typical
   `application/json` payload without needing streaming or multipart, and without needing a new
   request-size limit configuration change.
3. **No existing numeric precedent in this codebase to instead borrow** (the redirect endpoint's rate
   limit of 100 req/min, `feature-spec.md` Section 6, governs a completely different resource — abuse
   throttling on reads, not a single request's item count — so it is not treated as binding precedent
   here, just noted as the one place "100" already appears in this codebase's vocabulary, which is
   part of why 100 is a reasonable, unsurprising round number to choose independently).
4. **Flagged explicitly, as the raw request asked**: this cap is a **judgment call with no
   requirement-level backing**, and if the real-world use case is closer to "import 5,000 URLs from a
   CSV," 100 will be too small and either a higher cap, pagination-style chunked submission, or actual
   async processing (which would need new infrastructure this codebase doesn't have yet) should be
   revisited at Gate 1/Gate 2 — this is called out again in Section 5 below as a live open item for
   the human gate, not something this PRD is silently deciding away.

### 4.5 Non-Functional / Consistency Requirements (new, derived from the above)
- **FR-B2:** Batch item order in the response **must** match input order 1:1 (positional
  correlation is the only sane way for a caller to map failures back to their original input without
  requiring client-supplied per-item ids, which the raw request never mentioned wanting).
- **FR-B3:** Every per-item outcome — success or failure — **reuses exactly the existing field/error
  vocabulary** (Section 2.2) rather than inventing a parallel one, per `rules/architecture.md`
  Technology Agnosticism's spirit (no duplicate vocabularies for the same concept) and to keep a
  client that already parses single-create responses/errors able to reuse that parsing logic per
  batch item.
- **FR-B4:** The existing single-item `POST /api/v1/links` endpoint is **unchanged** — this is
  additive, not a breaking modification (`rules/data-layer.md` Migration Safety's "additive-first"
  principle, applied here to the API surface rather than the schema, since no schema change is
  involved at all per Section 2.4).

---

## 5. Posture Feasibility Verdict

**Filed posture: `mod`.** Checked whether the codebase evidence matches (per
`rules/posture-feasibility.md`, comparing the filed posture against language in the raw request
implying what kind of work is actually needed).

**Verdict: MATCH — no flag.**
- The raw request's own language ("Add a way to submit multiple URLs...") explicitly asks for new
  code to be written (a new endpoint + new service-layer logic), which is squarely `mod` territory
  (enhance existing service code), not `doc` (audit-only — would be a mismatch if this were asking
  only for documentation of existing bulk behavior, which doesn't exist) and not `dev`
  (`services-dev` implies a much larger/fresh service surface; this is a small, additive,
  single-endpoint enhancement to an already-complete service).
- Section 2/3's real-code read confirms `mod` is achievable exactly as scoped: reuse of
  `LinkService.createLink()`, no `data`-layer touch, one new controller method + a small number of
  new DTOs. Nothing discovered suggests the actual work is bigger (e.g., no existing async
  infrastructure implies "just add a queue" scope creep — Section 4.4 explicitly declines to invent
  that) or smaller (this is not literally zero-code doc work) than `mod` implies.

No RATIFY/EXPAND/NARROW/NO-GO gate extension is triggered by this check — the posture stands as
filed.

---

## 6. Role Feasibility (Pass 1) Verdict

**Filed role: `services-mod`, `layers_in_scope: [api, service]`** (confirmed against
`.claude/roles/services-mod/role-manifest.md`, which declares exactly `api`, `service` and describes
the role as "Patch/enhance/fix existing service code" — matching this feature's shape).

**Coarse check (Pass 1, per `rules/role-feasibility.md`): does the discovered code surface roughly
match the declared layers?**

| Layer | In role scope? | Does this feature touch it? | Evidence |
|---|---|---|---|
| `api` | Yes | Yes — one new endpoint (`POST /api/v1/links/batch`) + new request/response DTOs in `LinkController.java` / `api/dto/` | Section 3.1 |
| `service` | Yes | Yes — new bulk-oriented method reusing `LinkService.createLink()` per item; possibly one new lightweight exception type for batch-level (not per-item) validation (`EMPTY_BATCH`/`BATCH_TOO_LARGE`) living in `service/exception/` alongside the existing six | Section 2.4, 3.1 |
| `data` | **No** (excluded by role) | **No** — confirmed in Section 2.4: `ShortLinkEntity` already stores one row per link; a batch of N links needs zero schema change, zero new repository methods (the existing per-row `saveAndFlush` path, called N times, is exactly what's needed) | Section 2.4, 3.1 |

**Verdict: RATIFY — filed role fits the real code surface with no gap and no excess.**

No `EXPAND_LANES` is needed (the feature does not touch `data`, so `services-mod`'s exclusion of that
layer costs nothing here). No `NARROW_LANES` is warranted either (the feature genuinely needs both
`api` and `service`, not just one). No `NO-GO` condition applies. This is a clean match between filed
scope and actual required scope, confirmed against the real, freshly-read source — not assumed.

---

## 7. Summary for Gate 1

| Item | Resolution |
|---|---|
| Feature shape | enhancement |
| Role | services-mod — RATIFIED (Section 6) |
| Posture | mod — MATCH, no flag (Section 5) |
| Retry budget | 5 (no calibration history for this stack+role matrix row — Section 1) |
| New endpoint | `POST /api/v1/links/batch` |
| Request/response field names | Reuse existing `url`/`customCode` (request) and `shortCode`/`shortUrl`/`longUrl`/`createdAt`/`expiresAt` (success) / `code`/`message` (failure) vocabulary verbatim — Section 2.2, 4.3 |
| Partial-failure model | **Per-item independent ("partial success")** — `200 OK` envelope with per-item `results[]`, not all-or-nothing — Section 4.3. Flagged as a judgment call per the raw request's own ask. |
| Batch-size cap | **100 items**, enforced pre-processing (`EMPTY_BATCH` / `BATCH_TOO_LARGE`, both new 400 codes) — Section 4.4. Flagged as a judgment call with no requirement-level backing; revisit if real use cases need more (would likely require async infra this codebase doesn't have yet). |
| Layers touched | `api`, `service` only — no `data` change (Section 2.4, 6) |
| Layer-boundary integrity | Confirmed clean via direct grep of real imports (Section 2.3) — no violations to fix before starting |
