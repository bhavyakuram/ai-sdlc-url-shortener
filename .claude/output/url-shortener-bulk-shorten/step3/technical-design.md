---
agent: technical-design
inputs: [step2/feature-spec.md, step1/risk-register.md,
         service-java-spring/src/main/java/com/aisdlc/urlshortener/api/LinkController.java (real code, re-read this run),
         service-java-spring/src/main/java/com/aisdlc/urlshortener/service/LinkService.java (real code, re-read this run)]
stack: java-spring · role: services-mod (layers_in_scope: [api, service]) · mode: agentic · platform: none
---

# Technical Design: url-shortener-bulk-shorten (java-spring)

## 0. STEP-3 Agents Not Run — Explicit N/A (not silently skipped)

Per `rules/architecture.md` Write-Once Immutability and this run's task scope, the following
two STEP-3 agents were **not dispatched** for this feature. This is a deliberate N/A, confirmed
against real evidence, not an omission:

| Agent | Status | Evidence |
|---|---|---|
| `state-migration` | **N/A** | No schema change. `step1/risk-register.md` "Risks explicitly considered and ruled out": *"New data-layer schema risk — ruled out; impact-analysis.md and feasibility-report.md both independently confirm no schema change occurs."* This design confirms the same: every new class introduced below lives in `api` or `service`; nothing under `data/` (`ShortLinkEntity.java`, `ShortLinkRepository.java`, `ClickEventEntity.java`, `ClickEventRepository.java`) is touched, added to, or migrated. `rules/data-layer.md`'s Bottom-Up Schema Integration and Migration Safety sections have nothing to act on. |
| `refactor-migration` | **N/A** | No existing contract is reshaped. `POST /api/v1/links`, `GET /{code}`, `GET /api/v1/links/{code}/stats` — the three existing endpoints — keep identical request/response shapes, identical status codes, identical error vocabulary. `LinkController`'s three existing methods and `LinkService.createLink()`'s existing body are both unedited under this design (Section 1 below). There is no R1–R10 migration matrix to produce because there is nothing being migrated *away from* — this is a pure addition alongside the existing surface, confirmed by re-reading `LinkController.java`/`LinkService.java` directly (see Section 1). |

Both N/A determinations were already flagged at STEP-1 (`risk-register.md`, "Risks explicitly
considered and ruled out") and are re-confirmed here against the real, current source files
rather than taken on faith from STEP-1.

---

## 1. Component Decomposition — Exact File List

```
com.aisdlc.urlshortener
├── api/
│   ├── LinkController.java                 EXISTING — new method added (createBatch)
│   ├── ApiExceptionHandler.java             EXISTING — 2 new methods added
│   ├── WebConfig.java                       EXISTING — necessary DI/registration addition (Section 4)
│   ├── BatchRateLimitInterceptor.java       NEW
│   └── dto/
│       ├── BatchCreateLinkRequest.java      NEW
│       ├── BatchCreateLinkResponse.java     NEW
│       └── BatchItemResult.java             NEW
├── service/
│   ├── LinkService.java                     UNTOUCHED — zero edits, per R-BULK-2 mitigation (Section 2)
│   ├── BulkLinkOrchestrator.java            NEW — the self-invocation fix (Section 2)
│   ├── BulkLinkItem.java                    NEW — service-owned request-item shape (Section 5.1)
│   ├── BulkItemOutcome.java                 NEW — service-owned per-item outcome shape (Section 5.2)
│   ├── BatchRateLimiterService.java         NEW
│   └── exception/
│       ├── EmptyBatchException.java         NEW
│       └── BatchTooLargeException.java      NEW
└── data/                                    UNTOUCHED — no file added, no file edited (Section 0)
```

### 1.1 New files (class name, package, one-line purpose)

| # | Class | Package | Purpose |
|---|---|---|---|
| 1 | `BatchCreateLinkRequest` | `api.dto` | Request body record: `List<CreateLinkRequest> items`. Reuses the existing `CreateLinkRequest(url, customCode)` record verbatim for each item — feature-spec.md Section 0's mandated vocabulary reuse, and literally zero new field-shape code for the per-item request. |
| 2 | `BatchCreateLinkResponse` | `api.dto` | Response body record: `List<BatchItemResult> results, int successCount, int failureCount`. |
| 3 | `BatchItemResult` | `api.dto` | Per-item discriminated result: `status` plus either the `CREATED` field set or the `FAILED` field set (Section 5.3). |
| 4 | `BatchRateLimitInterceptor` | `api` | New `HandlerInterceptor`, IP-only key, batch path only (Section 3). |
| 5 | `BulkLinkOrchestrator` | `service` | The self-invocation fix: holds `LinkService` as a constructor dependency, drives the per-item loop through the injected proxy (Section 2). |
| 6 | `BulkLinkItem` | `service` | Service-layer-owned `(String url, String customCode)` record — the parameter type `BulkLinkOrchestrator.processBatch` actually accepts, deliberately **not** `api.dto.CreateLinkRequest` (Section 5.1 explains why this exists at all). |
| 7 | `BulkItemOutcome` | `service` | Service-layer-owned per-item result: either a `ShortLinkEntity` (success) or an `(errorCode, errorMessage)` pair (failure) — carries zero HTTP types, so the service layer never needs to know about `shortUrl` (Section 5.2). |
| 8 | `BatchRateLimiterService` | `service` | Bucket4j bucket manager keyed on source IP only, 20 req/60s, bounded LRU map — structurally identical pattern to the existing `RateLimiterService`, new instance/key/limit (Section 3). |
| 9 | `EmptyBatchException` | `service.exception` | Thrown by `BulkLinkOrchestrator` when `items` is null/empty. Maps to `400 EMPTY_BATCH`. |
| 10 | `BatchTooLargeException` | `service.exception` | Thrown by `BulkLinkOrchestrator` when `items.size() > 100`. Maps to `400 BATCH_TOO_LARGE`. |

### 1.2 Existing files — new methods only (not modified elsewhere)

| File | Change |
|---|---|
| `api/LinkController.java` | **New method** `createBatch(BatchCreateLinkRequest, HttpServletRequest)`, `@PostMapping("/api/v1/links/batch")`. Existing `createLink`, `redirect`, `stats`, and `buildShortUrl` methods are **not edited** — `createBatch` reuses `buildShortUrl` as-is (private helper, already generic over any code string). Constructor gains one new parameter (`BulkLinkOrchestrator`) — see Section 4 for why this one line of necessary DI wiring is called out explicitly rather than silently bundled into "new method." |
| `api/ApiExceptionHandler.java` | **Two new methods**: `handleEmptyBatch(EmptyBatchException)` → 400/`EMPTY_BATCH`, `handleBatchTooLarge(BatchTooLargeException)` → 400/`BATCH_TOO_LARGE`. Both follow the exact existing `build(HttpStatus, code, message)` pattern already used by every other handler in this file — zero new response-building logic. All seven existing `@ExceptionHandler` methods are untouched. |
| `api/WebConfig.java` | **Not a new-method change** — flagged explicitly, see Section 4. |

**`LinkService.java` is not in either table above — it receives zero changes of any kind.** This
is the load-bearing fact behind the self-invocation fix (Section 2): R-BULK-2 is closed by
*never adding a method to `LinkService.java` at all*, not by adding one carefully.

---

## 2. The Self-Invocation Fix (R-BULK-2 mitigation)

**Mechanism:** a new service-layer class, `service/BulkLinkOrchestrator.java`, takes `LinkService`
as a **constructor-injected dependency** (a distinct Spring bean reference, not `this`) and calls
`linkService.createLink(item.url(), item.customCode())` — through the injected proxy — once per
batch item, inside a loop, inside a `try/catch`.

```java
@Service
public class BulkLinkOrchestrator {

    private static final int MAX_BATCH_SIZE = 100;
    private final Logger log = LoggerFactory.getLogger(BulkLinkOrchestrator.class);
    private final LinkService linkService;

    public BulkLinkOrchestrator(LinkService linkService) {
        this.linkService = linkService;
    }

    public List<BulkItemOutcome> processBatch(List<BulkLinkItem> items) {
        if (items == null || items.isEmpty()) {
            throw new EmptyBatchException("items must be a non-empty array");
        }
        if (items.size() > MAX_BATCH_SIZE) {
            throw new BatchTooLargeException("items.length must not exceed " + MAX_BATCH_SIZE);
        }

        List<BulkItemOutcome> outcomes = new ArrayList<>(items.size());
        for (BulkLinkItem item : items) {
            try {
                // Through the injected proxy -- never this.createLink(...). This is the
                // entire R-BULK-2 mitigation: LinkService.java is never touched, so there is
                // no self-invocation call site to introduce in the first place.
                ShortLinkEntity link = linkService.createLink(item.url(), item.customCode());
                outcomes.add(BulkItemOutcome.success(link));
            } catch (InvalidUrlException ex) {
                outcomes.add(BulkItemOutcome.failure(ex.getErrorCode(), ex.getMessage()));
            } catch (InvalidCustomCodeShapeException ex) {
                outcomes.add(BulkItemOutcome.failure("INVALID_CUSTOM_CODE_SHAPE", ex.getMessage()));
            } catch (ReservedCodeException ex) {
                outcomes.add(BulkItemOutcome.failure("RESERVED_CODE", ex.getMessage()));
            } catch (CustomCodeTakenException ex) {
                outcomes.add(BulkItemOutcome.failure("CUSTOM_CODE_TAKEN", ex.getMessage()));
            } catch (CodeSpaceExhaustedException ex) {
                // Pathological case -- worth operator visibility even though caught in-loop,
                // mirroring ApiExceptionHandler's own log.error for this exception
                // (rules/coding-standards.md Logging). Never rethrown: R-BULK-4 requires this
                // exception stay inside the loop like every other per-item failure.
                log.error("CODE_SPACE_EXHAUSTED for one item in a batch request", ex);
                outcomes.add(BulkItemOutcome.failure("CODE_SPACE_EXHAUSTED", ex.getMessage()));
            }
            // Deliberately NOT catching LinkUnavailableException or bare Exception here --
            // see the "5 vs 6 vs 7" note below. An exception outside this named set is a real
            // bug, not a per-item outcome, and must still propagate to
            // ApiExceptionHandler.handleUnexpected -> 500 INTERNAL_ERROR (whole-request),
            // rather than being silently absorbed into a fake per-item result.
        }
        return outcomes;
    }
}
```

**Why this closes R-BULK-2 completely, not just "mostly":** Spring's `@Transactional` AOP proxy
only intercepts calls that arrive through the bean reference. Because `BulkLinkOrchestrator` is a
**different bean** than `LinkService`, every `linkService.createLink(...)` call in the loop above
is necessarily an inter-bean call through the proxy — there is no code path by which this design
could accidentally degrade into `this.createLink(...)`, because `BulkLinkOrchestrator` doesn't
`extend LinkService` and doesn't have a `createLink` method of its own to accidentally call
internally. `LinkService.java` itself is never edited (Section 1.2), so there is no self-invocation
call site to introduce in the first place — this is closing the risk structurally, not defensively.

**On "5 vs 6 vs 7" (exception-count discrepancy between `risk-register.md` and `feature-spec.md`,
resolved here by re-reading the real source):** `risk-register.md` R-BULK-4 says "all six of
`createLink()`'s possible exceptions"; `feature-spec.md` Section 3.1 says "all seven of
`createLink()`'s existing exception types." Neither is wrong — they're counting different things,
and re-reading `LinkService.java` end-to-end this run (every `throw new` site) resolves which:

- **5 exception classes** are actually reachable from `createLink()`: `InvalidUrlException`,
  `InvalidCustomCodeShapeException`, `ReservedCodeException`, `CustomCodeTakenException`,
  `CodeSpaceExhaustedException`. Traced call graph: `createLink` → `validateUrl` (throws
  `InvalidUrlException`, 3 possible `errorCode` values) → `validateCustomCodeShape` (throws
  `InvalidCustomCodeShapeException`) → `validateNotReserved` (throws `ReservedCodeException`) →
  `createWithCustomCode` (throws `CustomCodeTakenException`) / `createWithGeneratedCode` (throws
  `CodeSpaceExhaustedException`, two call sites, same exception type). `LinkUnavailableException`
  is thrown only by `redirectAndRecordClick` and `getStats` — grepped the whole file, confirmed it
  has zero throw sites inside `createLink()` or any method it calls. `risk-register.md`'s "six"
  appears to include `LinkUnavailableException` defensively even though its own text says
  "though not reachable from create" in the same sentence.
- **7 distinct `code` values** come out of those 5 classes, because `InvalidUrlException` alone
  carries 3 (`INVALID_URL_SCHEME` / `URL_TOO_LONG` / `URL_MALFORMED` via its own `getErrorCode()`).
  `feature-spec.md`'s "seven" is counting `code` values, not `catch` clauses — which is why the
  `catch` block above has exactly 5 clauses, not 6 or 7, and still surfaces all 7 codes correctly.

---

## 3. `BatchRateLimitInterceptor` Design (R-BULK-1 mitigation)

**Mechanism:** a second, independent `HandlerInterceptor`, registered on the batch path only.

| Property | Design decision | Rationale |
|---|---|---|
| Interceptor class | `api/BatchRateLimitInterceptor.java` (NEW) | Mirrors `RateLimitInterceptor`'s structure exactly (same `preHandle` shape, same header-writing, same fail-safe "never throws" contract) — a second sibling, not a modification of the first. |
| Bucket-management class | `service/BatchRateLimiterService.java` (NEW) | Mirrors `RateLimiterService`'s structure: bounded `LinkedHashMap` LRU (`MAX_TRACKED_KEYS = 50_000`, same bound as the existing service — feature-spec.md Section 5's storage-bound requirement), `synchronized` map access, Bucket4j `Bandwidth.classic` + `Refill.intervally`. |
| Key | **Source IP only** — `ClientIpResolver.resolve(request)`, the existing shared utility, reused with zero new IP-resolution logic. `tryConsume(String sourceIp)` — one string parameter, not the `(sourceIp, code)` two-parameter shape `RateLimiterService.tryConsume` uses. | There is no short code at request time for a batch request — a batch has no single "resource" pre-processing, and the batch limiter's whole purpose (R-BULK-1) is to bound *request rate*, not per-resource abuse. Keying on IP+something-that-doesn't-exist-yet isn't just unnecessary, it's structurally impossible before the per-item loop runs. |
| Limit | 20 requests / rolling 60s / source IP (`feature-spec.md` Section 5's Gate-approved number) | Not reused from `RateLimiterService.LIMIT_PER_MINUTE` (100) — a distinct constant, because it governs a categorically different unit of work (feature-spec.md Section 5, point 2: up to 200 DB round-trips per batch request vs. 1 read for the redirect endpoint). |
| Registration | `WebConfig.java`, `.addPathPatterns("/api/v1/links/batch")` only — batch path exclusively, via a **second, separate** `registry.addInterceptor(...)` call | Distinct registration, not a modification of the existing `.addPathPatterns("/*").excludePathPatterns("/api/**", "/actuator/**")` rule for `RateLimitInterceptor` (Section 4). The batch path already falls inside today's `excludePathPatterns("/api/**", ...)` for the *existing* interceptor — that exclusion is untouched; the *new* interceptor gets its own narrow, positive `addPathPatterns` instead of trying to carve an exception into the old rule. |
| Ordering guarantee | Runs in `preHandle`, which Spring MVC invokes before the `@RequestBody` argument resolver deserializes the request body (interceptors execute before `HandlerAdapter.handle()` invokes the controller method) | Satisfies feature-spec.md Section 4's fixed check order (rate-limit → parse → batch-size → per-item loop) as a structural property of where `HandlerInterceptor.preHandle` sits in the Spring MVC request lifecycle, not as something the interceptor's own code has to additionally enforce. |
| Over-limit response | `429`, reuses `RATE_LIMITED` code and `ErrorResponse` envelope verbatim, same `Retry-After` / `X-RateLimit-*` header convention, via the same `ObjectMapper`-injected pattern `RateLimitInterceptor` already uses | No new error code, no new envelope shape — feature-spec.md Section 5: "no new error code was needed or approved for this." |
| Failure mode | Never throws; `tryConsume` always returns a `ConsumptionProbe` | Identical fail-safe contract to `RateLimiterService` — a limiter bug must never surface as a 5xx. |

```java
@Service
public class BatchRateLimiterService {
    private static final int LIMIT_PER_MINUTE = 20;
    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final int MAX_TRACKED_KEYS = 50_000;

    private final Map<String, Bucket> buckets = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Bucket> eldest) {
            return size() > MAX_TRACKED_KEYS;
        }
    };

    public ConsumptionProbe tryConsume(String sourceIp) {
        Bucket bucket;
        synchronized (buckets) {
            bucket = buckets.computeIfAbsent(sourceIp, k -> newBucket());
        }
        return bucket.tryConsumeAndReturnRemaining(1);
    }

    public int limitPerMinute() { return LIMIT_PER_MINUTE; }

    private static Bucket newBucket() {
        Bandwidth limit = Bandwidth.classic(LIMIT_PER_MINUTE, Refill.intervally(LIMIT_PER_MINUTE, WINDOW));
        return Bucket.builder().addLimit(limit).build();
    }
}
```

`BatchRateLimitInterceptor.preHandle` is structurally identical to `RateLimitInterceptor.preHandle`
(resolve IP → `tryConsume` → write `X-RateLimit-*` headers → if not consumed, write 429 body via
`ErrorResponse.of(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", ...)` and return `false`; else
return `true`), with two differences: it calls `batchRateLimiterService.tryConsume(sourceIp)` (one
arg, not two) and it never calls `extractCode` (there is no code to extract on this path).

---

## 4. Necessary Exceptions to "New Methods Only" — Flagged, Not Silent

Two existing files need more than a pure new-method addition, because Spring constructor
injection requires every dependency to be declared in the constructor signature. Both are called
out explicitly here rather than silently folded into "just a new method":

1. **`LinkController.java`** — constructor gains one new parameter, `BulkLinkOrchestrator
   bulkLinkOrchestrator`, alongside the existing `LinkService linkService` parameter. This is
   unavoidable: `createBatch` cannot be added as a new method without the orchestrator it calls
   being available as a field. The three existing methods and the existing `buildShortUrl` helper
   are otherwise byte-for-byte unchanged.
2. **`WebConfig.java`** — constructor gains one new parameter, `BatchRateLimitInterceptor
   batchRateLimitInterceptor`, and `addInterceptors` gains one new `registry.addInterceptor(...)`
   statement (Section 3's registration row) alongside — not replacing — the existing
   `RateLimitInterceptor` registration. `WebConfig` has exactly one method whose entire job is
   "register interceptors"; there is no way to add a second interceptor registration without
   touching that method's body, and no separate "new method" shape is available here the way
   `LinkController`/`ApiExceptionHandler` afford one. The existing registration's own
   `addPathPatterns`/`excludePathPatterns` arguments are not edited.

Both changes are pure additions (new parameter, new statement) — neither existing registration,
existing method body line, nor existing constructor parameter is deleted or altered.

---

## 5. Cross-Layer Data Shapes — Why Three New Small Types Exist

### 5.1 `BulkLinkItem` (service) — not `api.dto.CreateLinkRequest`

`BulkLinkOrchestrator.processBatch` takes `List<BulkLinkItem>`, **not**
`List<CreateLinkRequest>`, even though the two records would have identical fields
(`url`, `customCode`). This is deliberate, not an oversight: `CreateLinkRequest` lives in
`api.dto` — a package one layer *above* `service` in the `api → service → data` dependency
direction (`rules/architecture.md` Dependency Direction: "No layer may import from a layer above
it"). If `BulkLinkOrchestrator` accepted `CreateLinkRequest` directly, `service/` would carry an
`import com.aisdlc.urlshortener.api.dto.CreateLinkRequest`, an upward import — a real,
mechanically-checkable violation (Section 6). `BulkLinkItem` is a trivial, service-owned record
with the same two fields; `LinkController.createBatch` does the one-line mapping
(`request.items().stream().map(i -> new BulkLinkItem(i.url(), i.customCode())).toList()`) at the
boundary, exactly where a layer-crossing translation belongs.

### 5.2 `BulkItemOutcome` (service) — carries no HTTP types

`BulkItemOutcome` holds either a `ShortLinkEntity` (success) or an `(errorCode, errorMessage)`
pair (failure) — it does **not** hold a `shortUrl`. `shortUrl` is built from `HttpServletRequest`
(`ServletUriComponentsBuilder`, an HTTP/servlet concept) — exactly the same reason
`LinkService.createLink` itself returns a bare `ShortLinkEntity` today and lets
`LinkController.createLink` build `shortUrl` afterward via the existing private
`buildShortUrl(HttpServletRequest, String)` helper. `BulkLinkOrchestrator` follows the identical
precedent: it returns `List<BulkItemOutcome>` with no URL-building of its own, and
`LinkController.createBatch` calls the existing `buildShortUrl` helper once per successful outcome
to assemble the final `BatchItemResult`. This keeps `shortUrl` construction where it already lives
today — the `api` layer — and adds zero new HTTP-aware code to `service/`.

```java
public record BulkItemOutcome(ShortLinkEntity link, String errorCode, String errorMessage) {
    public static BulkItemOutcome success(ShortLinkEntity link) {
        return new BulkItemOutcome(link, null, null);
    }
    public static BulkItemOutcome failure(String errorCode, String errorMessage) {
        return new BulkItemOutcome(null, errorCode, errorMessage);
    }
    public boolean isSuccess() { return link != null; }
}
```

### 5.3 `BatchItemResult` (api.dto) — exact wire-shape match, null fields suppressed

```java
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record BatchItemResult(String status, String shortCode, String shortUrl, String longUrl,
                               Instant createdAt, Instant expiresAt, String code, String message) {

    public static BatchItemResult created(ShortLinkEntity link, String shortUrl) {
        return new BatchItemResult("CREATED", link.getCode(), shortUrl, link.getTargetUrl(),
                link.getCreatedAt(), link.getExpiresAt(), null, null);
    }

    public static BatchItemResult failed(String code, String message) {
        return new BatchItemResult("FAILED", null, null, null, null, null, code, message);
    }
}
```

`@JsonInclude(NON_NULL)` is required at the class level (not a global `ObjectMapper` config
change, which would touch existing serialization behavior for every other endpoint) so a
`CREATED` item's JSON has no stray `"code":null,"message":null`, and a `FAILED` item's JSON has no
stray `"shortCode":null,...` — matching `feature-spec.md` Section 3.1's example response exactly,
field for field.

`LinkController.createBatch` assembles the final response:

```java
@PostMapping("/api/v1/links/batch")
public ResponseEntity<BatchCreateLinkResponse> createBatch(@RequestBody BatchCreateLinkRequest request,
                                                              HttpServletRequest httpRequest) {
    List<BulkLinkItem> items = request.items().stream()
            .map(i -> new BulkLinkItem(i.url(), i.customCode()))
            .toList();
    List<BulkItemOutcome> outcomes = bulkLinkOrchestrator.processBatch(items);

    List<BatchItemResult> results = outcomes.stream()
            .map(o -> o.isSuccess()
                    ? BatchItemResult.created(o.link(), buildShortUrl(httpRequest, o.link().getCode()))
                    : BatchItemResult.failed(o.errorCode(), o.errorMessage()))
            .toList();
    long successCount = results.stream().filter(r -> "CREATED".equals(r.status())).count();

    return ResponseEntity.ok(new BatchCreateLinkResponse(results, (int) successCount,
            results.size() - (int) successCount));
}
```

`EmptyBatchException`/`BatchTooLargeException` thrown by `bulkLinkOrchestrator.processBatch`
propagate up through this method uncaught — exactly like every other whole-request validation
failure in this codebase — to `ApiExceptionHandler`'s two new handlers (Section 1.2).

---

## 6. Layer Boundary Confirmation

**Yes — the orchestrator lives in `service/`, not `api/`.** Justification:

1. **It is business orchestration logic, not an HTTP concern.** `BulkLinkOrchestrator` decides
   *what happens* for a batch of link-creation requests (validate size, attempt each item
   independently, collect outcomes) — the same category of responsibility `LinkService` already
   holds for a single link. Nothing in it reads or writes `HttpServletRequest`,
   `HttpServletResponse`, status codes, or headers. Per `rules/architecture.md` Dependency
   Direction ("the data layer must not know about HTTP concerns; the API layer must not embed
   persistence logic"), the corollary this design leans on is the same one `LinkService` already
   establishes: business logic that doesn't need HTTP types shouldn't hold them, and `service/` is
   exactly where that logic already lives for the single-create path.
2. **It depends on `LinkService` (`service` → `service`, same layer), not the reverse.** No `api`
   class becomes a dependency *of* `service` — `BulkLinkOrchestrator`'s only collaborator is
   another `service`-layer bean. The dependency arrow stays `api → service → data`;
   `BulkLinkOrchestrator` sits inside the `service` node of that arrow, not athwart it.
3. **It is checked mechanically, not just asserted.** `rules/architecture.md` Section
   "Dependency Direction" requires this be checked by static analysis at STEP-6.1, not merely
   claimed here. This design makes that check trivially satisfiable: `BulkLinkOrchestrator.java`,
   `BulkLinkItem.java`, and `BulkItemOutcome.java` (Section 5.1/5.2) contain **zero** imports from
   `com.aisdlc.urlshortener.api.*` by construction — `BulkLinkItem` exists specifically so the
   orchestrator never needs to import `api.dto.CreateLinkRequest` (Section 5.1). A grep for
   `import com.aisdlc.urlshortener.api` across the three new `service/` files should return zero
   matches once generated at STEP-4 — a concrete, verifiable predicate for `build-verdict`/
   static-analysis to check, not a promise.
4. **Every other new/changed file's layer is consistent with its own job**, listed for
   completeness: `BatchRateLimitInterceptor` (HTTP interceptor, HTTP concern) → `api/`;
   `BatchRateLimiterService` (bucket bookkeeping, no HTTP types, mirrors `RateLimiterService`) →
   `service/`; `BatchCreateLinkRequest`/`BatchCreateLinkResponse`/`BatchItemResult` (wire DTOs,
   Jackson-serialized, HTTP request/response shapes) → `api/dto/`; `EmptyBatchException`/
   `BatchTooLargeException` (thrown by `service/`, mirrors the existing pattern where every one of
   `LinkService`'s six exception types also lives in `service/exception/` despite being HTTP-status
   -mapped one layer up in `ApiExceptionHandler`) → `service/exception/`.

---

## 7. Traceability Back to Risk Register

| Risk | Design element | Section |
|---|---|---|
| R-BULK-1 (rate-limit amplification) | `BatchRateLimitInterceptor` + `BatchRateLimiterService`, 20 req/60s/IP, registered on `/api/v1/links/batch` only | 3 |
| R-BULK-2 (self-invocation) | `BulkLinkOrchestrator` as a separate bean calling `linkService.createLink(...)` through the injected proxy; `LinkService.java` receives zero edits | 2 |
| R-BULK-3 (up to 200 DB round-trips/request) | Unchanged 100-item cap (enforced by `BatchTooLargeException`); rate limiter sized against this cost (Section 3) | 5.1, 3 |
| R-BULK-4 (per-item exception catching) | 5-clause `catch` in `BulkLinkOrchestrator.processBatch` covering all 7 reachable `code` values; nothing escapes to `ApiExceptionHandler` except a genuine bug | 2 |
| R-BULK-5 (validation ordering) | Rate limiter in `preHandle` (before body deserialization) → `EmptyBatchException`/`BatchTooLargeException` checked before the per-item loop starts | 3, 2 |
