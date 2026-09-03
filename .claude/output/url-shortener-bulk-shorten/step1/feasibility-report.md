# STEP-1: Feasibility Report — url-shortener-bulk-shorten

**Agent:** feasibility · **Input:** `prework/prd-v0.md` (Section 4.3/4.4 design), real source
under `service-java-spring/src/main/java/com/aisdlc/urlshortener/`.

## Verdict: **BUILDABLE AS PROPOSED — no blocker.**

## Evidence

### 1. The one piece of reused logic exists exactly as the PRD claims
`service/LinkService.java:67-79`, verbatim:
```java
@Transactional
public ShortLinkEntity createLink(String url, String customCode) {
    validateUrl(url);
    ...
    if (customCode != null && !customCode.isBlank()) {
        ...
        return createWithCustomCode(customCode, url, now, expiresAt);
    }
    return createWithGeneratedCode(url, now, expiresAt);
}
```
Signature `(String url, String customCode) -> ShortLinkEntity`, called once per batch item, is
sufficient by itself to satisfy the proposed per-item semantics (validation, custom-code
handling, generated-code handling, all six existing exception types) with **zero changes** to
this method's body. Confirmed no overload, no batch-aware variant needed at the `data` layer —
`data/ShortLinkEntity.java` and `data/ShortLinkRepository.java` are unchanged by a batch of N
independent rows (one row = one link, exactly today's shape).

### 2. Existing DTO vocabulary is genuinely reusable, not just similarly named
`api/dto/LinkResponse.java`, `api/dto/ErrorResponse.java`, `api/dto/CreateLinkRequest.java` (all
read verbatim) already carry every field the per-item batch result needs
(`shortCode`/`shortUrl`/`longUrl`/`createdAt`/`expiresAt` for success, `code`/`message` for
failure). No new field vocabulary has to be invented — a per-item result record can wrap these
directly.

### 3. Existing exception→code mapping in `ApiExceptionHandler.java` covers every per-item
failure the reused `createLink()` can throw
Verbatim handler list: `InvalidUrlException` (400, code from exception), `InvalidCustomCodeShapeException`
(400, `INVALID_CUSTOM_CODE_SHAPE`), `ReservedCodeException` (400, `RESERVED_CODE`),
`CustomCodeTakenException` (409, `CUSTOM_CODE_TAKEN`), `LinkUnavailableException` (404 — not
reachable from `createLink`), `CodeSpaceExhaustedException` (503, `CODE_SPACE_EXHAUSTED`). All six
are ordinary Java exception types the batch method can catch **per iteration** without any new
service-layer surface — but see the routing caveat in Finding F1 below, which is a build-order
constraint, not a blocker.

### 4. No `data`-layer change required (confirmed independently)
`data/ShortLinkEntity.java` maps 1:1 to `short_link`, one row per link, `code` column `UNIQUE`.
Nothing about "many links submitted together" needs a schema concept — N batch items are N
ordinary inserts through the existing repository. `schema.sql`/`application.yml` need no edits.

### 5. No web-layer request-size obstacle
`application.yml` sets no custom `server.tomcat.max-http-form-post-size` or
`spring.servlet.multipart` overrides — Spring Boot's default `server.max-http-header-size`/body
limits comfortably exceed a ~200KB worst-case 100-item JSON body (each `url` capped at 2048 chars
per `MAX_URL_LENGTH` in `LinkService.java:36`). No configuration change needed to accept the
proposed request shape.

## Findings (non-blocking, but material to how STEP-3/4 should build this)

**F1 — Build-order constraint, not a blocker:** the six exceptions above are normally caught by
`@RestControllerAdvice ApiExceptionHandler` at the *request* level (`ApiExceptionHandler.java`
verbatim: `@ExceptionHandler(InvalidUrlException.class)` etc., each producing one whole HTTP
response). For partial-success semantics, the new batch method **must** catch these exceptions
itself, per loop iteration, and never let them propagate up to `ApiExceptionHandler` — if a bad
item's exception is allowed to propagate, Spring's normal dispatch would turn the entire batch
request into a single 400/409 response instead of a `200` with a mixed `results[]`, defeating the
partial-success model the PRD designs for. This is a correctness requirement for STEP-4's
generator to honor, not a feasibility blocker — the exceptions are ordinary, catchable Java types
available for exactly this purpose.

**F2 — Where the loop lives affects transactional correctness (see risk-register.md R-BULK-2 for
detail):** `createLink()`'s `@Transactional` boundary is enforced by Spring's AOP proxy, which
only intercepts calls made *through* the injected bean reference, not self-invocation within the
same class. If STEP-4 implements the batch loop as a new method **inside** `LinkService.java`
calling `this.createLink(...)`, each iteration's `@Transactional` semantics silently stop applying
(no proxy interception on self-calls). If the loop instead lives in `LinkController.java` or a new,
separate service class that holds `LinkService` as a constructor-injected dependency and calls
`linkService.createLink(...)` (through the proxy), transactional semantics are preserved exactly as
today. This does not block buildability either way — it determines whether the implementation is
*correct*, and is called out here so STEP-3 (Technical Design) makes the call deliberately rather
than by accident.

## Conclusion
No blocker of any kind — technical, dependency, or architectural — prevents building
`POST /api/v1/links/batch` as scoped in `prework/prd-v0.md` Section 4.3/4.4. The two findings
above (F1, F2) are implementation-order/design-correctness notes for STEP-3/STEP-4, carried
forward into `risk-register.md`.
