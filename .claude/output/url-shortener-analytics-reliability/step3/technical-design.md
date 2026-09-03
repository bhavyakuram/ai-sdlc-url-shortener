---
agent: technical-design
inputs: [step2/feature-spec.md, step2/acceptance-criteria.md,
         service-java-spring/src/main/java/com/aisdlc/urlshortener/service/LinkService.java (real code, re-read this run),
         service-java-spring/src/main/java/com/aisdlc/urlshortener/data/ClickEventRepository.java (real code, re-read this run),
         service-java-spring/src/main/java/com/aisdlc/urlshortener/api/LinkController.java (real code, re-read this run)]
stack: java-spring · role: services-mod (layers_in_scope: [api, service]) · mode: agentic · platform: none
---

# Technical Design: url-shortener-analytics-reliability (java-spring)

## 0. STEP-3 Agents Not Run — Explicit N/A

| Agent | Status | Evidence |
|---|---|---|
| `state-migration` | **N/A** | No schema change. `ClickEventRepository` (re-read this run) already extends `JpaRepository<ClickEventEntity, Long>`, which already exposes `saveAndFlush` — the exact method this design uses (Section 1) — with zero interface edits needed. `data/ClickEventEntity.java`/`data/ClickEventRepository.java` are both untouched. |
| `refactor-migration` | **N/A** | No existing contract is reshaped. `GET /{code}`'s request, response shape, and status codes are unchanged (feature-spec.md C3) — this is a failure-path-only fix inside one existing method body, not a contract migration. |
| `api-contract` | **N/A** | No new endpoint, no changed request/response DTO. `LinkController.redirect()` (line 95) is not edited at all — the fix is entirely inside `LinkService.redirectAndRecordClick`. |

---

## 1. The Fix — Exact Code Shape

**File:** `service-java-spring/src/main/java/com/aisdlc/urlshortener/service/LinkService.java`,
method `redirectAndRecordClick` (lines 139-153, re-read verbatim this run).

**Current code (unchanged lines shown for boundary clarity):**
```java
@Transactional
public ShortLinkEntity redirectAndRecordClick(String code, String referrer, String sourceIp) {
    ShortLinkEntity link = shortLinkRepository.findByCode(code)
            .orElseThrow(() -> new LinkUnavailableException("No active short link for code: " + code));
    Instant now = Instant.now();
    if (link.isExpired(now)) {
        throw new LinkUnavailableException("Short link has expired: " + code);
    }

    String country = geoLookupService.lookupCountry(sourceIp);
    ClickEventEntity click = new ClickEventEntity(link.getId(), now, referrer, country);
    clickEventRepository.save(click);        // line 150 -- the one line this fix touches

    return link;
}
```

**Fixed code — only the write line changes shape, everything above and the `return` are byte-for-byte identical:**
```java
@Transactional
public ShortLinkEntity redirectAndRecordClick(String code, String referrer, String sourceIp) {
    ShortLinkEntity link = shortLinkRepository.findByCode(code)
            .orElseThrow(() -> new LinkUnavailableException("No active short link for code: " + code));
    Instant now = Instant.now();
    if (link.isExpired(now)) {
        throw new LinkUnavailableException("Short link has expired: " + code);
    }

    String country = geoLookupService.lookupCountry(sourceIp);
    ClickEventEntity click = new ClickEventEntity(link.getId(), now, referrer, country);
    try {
        clickEventRepository.saveAndFlush(click);
    } catch (RuntimeException ex) {
        log.error("Failed to record click for code '{}' (linkId={}, occurredAt={}) -- "
                + "redirect will proceed without recording this click.",
                code, link.getId(), now, ex);
    }

    return link;
}
```
Plus a `private static final Logger log = LoggerFactory.getLogger(LinkService.class);` field
(`LinkService.java` currently has none — `ApiExceptionHandler.java` and `GeoLookupService.java` both
already follow this exact SLF4J `Logger`/`LoggerFactory` pattern; `LinkService` picks up the same
two imports, `org.slf4j.Logger` / `org.slf4j.LoggerFactory`, adding a `log` field consistent with the
rest of the codebase — `rules/coding-standards.md` Logging, "no `print`/`System.out`").

**Why `saveAndFlush`, not the existing `save` — this is not a stylistic substitution, re-derived
from re-reading the actual failure mode:**

`JpaRepository.save()` does not, by itself, guarantee the physical `INSERT` happens before the
method call returns. Under a plain `save()`, Hibernate is free to defer the flush to the *next*
auto-flush point or to transaction commit — and since `redirectAndRecordClick` does no further
repository read after this line, the next flush point would be **commit**, which for a
`@Transactional` proxy method happens in the AOP interceptor's `after-return` handling, i.e. *after
this method body has already returned and the `try/catch` block has already exited*. `prd-v0.md`
Section 2.4 identifies the actual failure mode as a JDBC-level failure under burst load (lock
timeout, connection-pool exhaustion) — exactly the kind of failure that manifests at flush/commit
time, not at the moment `save()` merely stages the entity in the persistence context. A `try/catch`
wrapped around a bare `save()` call would therefore give the *appearance* of isolating the write
while actually catching nothing for the real failure mode this fix exists to address — the
exception would still surface later, uncaught, from inside the transactional proxy's commit,
propagating to `LinkController.redirect()` exactly as it does today.

`saveAndFlush` forces the `INSERT` to execute synchronously, inside this method's own stack frame,
so a JDBC-level failure throws *inside* the `try` block where this fix can actually catch it. This
is not a new pattern invented for this fix — it is the exact convention this same file already uses
for its other two guarded writes, `createWithGeneratedCode` (lines 88-114) and `createWithCustomCode`
(lines 117-127), both of which use `saveAndFlush` specifically so their `DataIntegrityViolationException`
catch blocks have something synchronous to catch. This fix applies the identical, already-established
technique to a different exception type (`RuntimeException`, broader than
`DataIntegrityViolationException` — see below) for a different reason (JDBC/timeout resilience, not
a unique-constraint race), but the underlying "force the flush into the try block" mechanism is the
same one this codebase already relies on.

**Why `catch (RuntimeException)`, not the file's own narrower `DataIntegrityViolationException`
precedent:** the two existing guarded writes in this file catch `DataIntegrityViolationException`
specifically because their risk is a *known, single* constraint-violation type (`short_link.code`'s
`UNIQUE` constraint, `prd-v0.md` Section 2.2). The click-write path has no such single, named risk —
`prd-v0.md` Section 2.4 identifies the exposure as "a lock timeout, connection-pool exhaustion...
H2 file-mode write contention," which can surface through several distinct subtypes of Spring's
`org.springframework.dao.DataAccessException` hierarchy (`CannotAcquireLockException`,
`DataAccessResourceFailureException`, `QueryTimeoutException`, etc. — all `RuntimeException`s, none
of them `DataIntegrityViolationException`). Catching the common `RuntimeException` supertype is the
correct scope here specifically *because* the failure mode is heterogeneous and not yet enumerable
to one exception class, not because broad catching is generally preferred — and `feature-spec.md`
Section 3 already establishes why this catch's *placement* (only around the write, entered only after
the lookup/expiry checks have already passed) keeps it from ever intercepting `LinkUnavailableException`,
so the breadth of the exception type caught here does not reopen C4.

**Log context satisfies `rules/coding-standards.md` Logging and `feature-spec.md` C2 concretely:**
`code` (which short link), `link.getId()` (which row the click would have referenced — useful once
`getStats` is queried and the count looks low), and `now` (when it happened) are exactly the three
facts an operator would need to correlate this log line against a specific missing row without
re-running the request — the causing exception (`ex`) is passed as the final SLF4J varargs argument,
matching this codebase's own existing convention (`ApiExceptionHandler.handleUnexpected`,
`GeoLookupService.lookupCountry`'s `log.warn(..., ex)` calls, both re-read this run).

---

## 2. Layer Boundary Confirmation

**No layer boundary is crossed or newly introduced.** The entire fix is inside one existing method
of `service/LinkService.java` — no new file, no new class, no new import from `api` or `data` beyond
what the method already imports (`ClickEventRepository`, already a field). `rules/architecture.md`
Dependency Direction (`api -> service -> data`) is unaffected: `LinkController.redirect()` (line
95-102) is not edited, so the `api` layer's call site into `service` is byte-for-byte unchanged.

---

## 3. Traceability to feature-spec.md

| Contract clause | Design element | Section |
|---|---|---|
| C1 (redirect survives click-write failure) | `try { clickEventRepository.saveAndFlush(click); } catch (RuntimeException ex) { ... }` wrapped only around the write | 1 |
| C2 (diagnosable log) | `log.error` with `code`, `link.getId()`, `now`, and `ex` | 1 |
| C3 (no response-shape/status change) | Zero edits to `LinkController.redirect()` | 2 |
| C4 (`LinkUnavailableException` still propagates) | Try/catch begins after both `orElseThrow` (line 141-142) and the expiry check (line 144-146); `RuntimeException` catch cannot reach either, because both throw before the try block is entered, not because the exception type is excluded | 1 |
| C5 (normal path unchanged) | `saveAndFlush` still persists the row exactly as `save` did when nothing fails; `return link` unchanged | 1 |

---

## 4. Design Decision: `@Async` Considered and Rejected

**The question:** `prd-v0.md` Section 2.5 Interpretation B names two options for isolating the
click write — "catch-and-log around `clickEventRepository.save(click)`, **or** move it out of the
redirect's transaction boundary." Moving it off the request thread entirely (`@Async`, or a
queue/executor handoff) is the most literal reading of "move it out of the transaction boundary," so
it has to be actively re-derived and rejected here, not merely dismissed by citing the prior pass's
conclusion.

**Re-derivation, from the actual evidence available in this codebase, not by analogy:**

1. **No async infrastructure exists in this codebase today, and adding it is a new capability, not
   a fix.** Grepped the entire `service-java-spring` tree for `@EnableAsync`, `@Async`,
   `TaskExecutor`, `ThreadPoolTaskExecutor` this run — zero matches. Making the click write
   asynchronous would require introducing Spring's async machinery from scratch: an `@EnableAsync`
   configuration class, a bounded `TaskExecutor` bean (an *unbounded* one, e.g. the default
   `SimpleAsyncTaskExecutor`, spawns a new thread per invocation with no cap at all — under the
   exact "hit a lot at once" burst scenario `prd-v0.md` is worried about, that is a new, self-inflicted
   resource-exhaustion risk, arguably worse than the one being fixed), and an
   `AsyncUncaughtExceptionHandler` (Spring's default for a `void` `@Async` method is to log-and-drop
   the exception via `SimpleAsyncUncaughtExceptionHandler` — which happens to still satisfy C2's
   "logged" requirement by luck, but only if someone remembers to configure or verify it; the
   synchronous try/catch in Section 1 gets the same guarantee for free, with no new configuration
   surface to get wrong).
2. **`@Async` reintroduces a self-invocation risk this codebase has already hit once, this project.**
   Spring's `@Async` proxy, like `@Transactional`, only intercepts calls that arrive through the
   bean's proxy — a call to `this.recordClickAsync(...)` from inside `LinkService` would silently run
   synchronously, not asynchronously, with no compile-time signal that anything is wrong. This is
   the exact class of bug `url-shortener-bulk-shorten/step1/risk-register.md` R-BULK-2 already
   identified and fixed for a different method (`createLink`) by introducing a *separate* bean
   (`BulkLinkOrchestrator`) purely to get an inter-bean call. Doing this correctly for `@Async` would
   mean extracting the click-write into its own new service class as well — real, non-trivial
   surface area (a new file, a new bean, a new constructor-injected dependency wired through
   `LinkService`) to fix a defect the try/catch in Section 1 closes with a three-line change to a
   method that already exists.
3. **Async changes an observable behavior the task's own AC42 pins: "click IS still recorded when
   nothing fails."** Today, a click is durably committed and immediately visible to
   `GET /api/v1/links/{code}/stats` by the time the `302` response reaches the caller — the write and
   the redirect commit in the same transaction. Moving the write off-thread makes click-visibility
   *eventually* consistent: a stats call issued immediately after the redirect could now race the
   async write and undercount. Nothing in `prd-v0.md` asks for this change, and introducing it as a
   side effect of the reliability fix would be changing tested, working behavior to fix a problem
   that doesn't require changing it — the try/catch in Section 1 keeps click-visibility exactly as
   synchronous and immediate as it is today (AC42), because the write still happens inline; it's only
   the *exception*, not the write's timing, that's handled differently now.
4. **The capacity question `@Async` would actually be answering isn't the one this run confirmed.**
   `prd-v0.md` Section 2.5 Interpretation C — raw throughput under sustained concurrent load — is
   explicitly flagged as *not settled* by reading the code: "there is no load-test harness anywhere
   in this repository... this would need an actual concurrent-request test to confirm empirically,
   not just inferred from source." `@Async` is a throughput/latency lever (get the request thread
   back faster under load); the finding this run actually confirmed and is fixing is an
   *availability-coupling* defect (Interpretation B), which the synchronous try/catch fully resolves
   on its own — C1 (redirect succeeds even when the write throws) does not require the write to run
   on a different thread, only that its failure not propagate. Reaching for `@Async` here would be
   solving for a capacity concern that has no supporting measurement in this run, at the cost of the
   three real risks above, when the confirmed defect is fully closed without it.

**Conclusion: rejected.** The try/catch in Section 1 is the complete fix for the confirmed defect
(C1-C5), introduces zero new infrastructure, zero new files, zero new beans, and zero change to the
normal-path timing/visibility behavior AC42 pins. `@Async` would be scope creep — a speculative
performance change bundled into an availability-coupling bug fix, addressing a capacity question
`prd-v0.md` itself says this run did not and could not settle.
