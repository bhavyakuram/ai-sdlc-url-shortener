# STEP-2 Specification: Feature Spec — url-shortener-analytics-reliability

**Phase:** STEP-2 · **Agent:** `feature-spec` (skill: `skills/step2/feature-spec/SKILL.md`)
**Stack:** java-spring · **Role:** services-mod (`layers_in_scope: [api, service]`) — **EXPAND_LANES
from `services-doc`, ratified at Gate 1** (`prework/prd-v0.md` Section 3, MISMATCH → EXPAND) ·
**Mode:** agentic · **Platform:** none
**Inputs read (full):** `prework/prd-v0.md` (all sections — this run has no separate
`step1/feasibility-report.md` / `step1/risk-register.md`; see Section 0 below for why),
`service-java-spring/src/main/java/com/aisdlc/urlshortener/service/LinkService.java` (real code,
re-read this dispatch, lines 139-153), `service-java-spring/.../api/LinkController.java` (real
code, re-read this dispatch, lines 94-102), `service-java-spring/.../api/ApiExceptionHandler.java`
(real code, re-read this dispatch, lines 82-86)

---

## 0. STEP-1 Agents Not Run — Explicit N/A, Not a Silent Skip

Per the task brief, this run goes directly from `prework/prd-v0.md` to STEP-2/STEP-3 for a small,
already-bounded brownfield fix. `step1/feasibility-report.md` and `step1/risk-register.md` do not
exist for this feature (confirmed: only `prework/prd-v0.md`, `_role-context.yaml`,
`_decisions.yaml`, `_run-log.md` exist under `.claude/output/url-shortener-analytics-reliability/`).
This is not treated as a missing dependency because `prework/prd-v0.md` Section 2 already performed
the equivalent discovery work end-to-end and evidence-first: it re-read the real method, traced the
real failure path (Section 2.4), checked the schema for a collision surface (Section 2.3), and ruled
out silent data loss structurally rather than by assumption (Section 2.4's "Concrete answer"). There
is exactly one finding to act on (Section 1 below), not a set of risks needing a separate register —
so this spec treats `prd-v0.md` Section 2 as its risk/feasibility input directly, cited by section
number throughout, rather than re-deriving a parallel STEP-1 artifact for a fix this narrow.

---

## 1. The Finding, Restated as a Behavior Contract (not re-litigated, carried forward from prd-v0.md Section 2.4-2.5)

**What's wrong today:** `LinkService.redirectAndRecordClick` (lines 139-153) is one `@Transactional`
method. The click-event write (`clickEventRepository.save(click)`, line 150) and the link lookup
that produces the redirect target share the same transaction boundary. If the click write fails for
any reason, the exception propagates uncaught through `LinkController.redirect()` (lines 94-102, no
local try/catch there either — re-confirmed this dispatch) to `ApiExceptionHandler.handleUnexpected`
(lines 82-86), which returns `500 INTERNAL_ERROR` — **the redirect fails, not just the click record.**

**What "fixed" means, precisely (the behavior contract this spec fixes in place):**

| # | Contract clause | Why it's in scope |
|---|---|---|
| C1 | The redirect (`GET /{code}`) must succeed — return `302 Found` to the correct `longUrl` — even when the click-event write throws. | The core defect: `prd-v0.md` Section 2.4/2.5 Interpretation B — "redirect availability should not depend on analytics-write success." |
| C2 | The click-write failure must be logged with enough context to diagnose without re-running the request — at minimum the short `code`, the resolved `link` id, and the attempted `occurredAt` timestamp, plus the causing exception. | `rules/coding-standards.md` Logging ("every service-layer method that can fail logs the failure with enough context") and No Silent Catches (an empty catch block is a BLOCKER). |
| C3 | No existing status code or response shape for `GET /{code}` may change. A successful redirect is still exactly `302 Found` with the existing `Location` header built from `link.getTargetUrl()` — nothing added, nothing removed, for the success path. | Task brief hard constraint; `LinkController.redirect()` (lines 94-102) is not to be edited beyond what threading the fix through requires (Section 3, technical-design.md). |
| C4 | `LinkUnavailableException` (unknown code, line 142; expired code, line 145) must continue to propagate exactly as today — `404 CODE_NOT_FOUND` via `ApiExceptionHandler.handleLinkUnavailable` (unchanged). Only the click-write step is isolated; the lookup/expiry checks are not touched and their failures are not swallowed. | Task brief hard constraint; this is also structurally guaranteed by isolating only the write statement rather than the whole method body (technical-design.md Section 1) — the lookup/expiry checks run and throw *before* any isolation boundary begins. |
| C5 | When nothing fails, the click is still recorded exactly as today — one `ClickEventEntity` row per successful redirect, immediately visible to `GET /api/v1/links/{code}/stats`. | Regression guard: this is a fix to the failure path only; the normal path (the overwhelming majority of requests) must be provably unchanged. |

**What is explicitly out of scope (carried forward, not re-derived here — see technical-design.md
Section 2 for the re-derivation of why):**
- Any change to `GeoLookupService` — it already fails soft and never throws (`prd-v0.md` Section
  2.4, re-confirmed unchanged this dispatch).
- Any change to `click_event`'s schema, or a `state-migration` dispatch — no schema change is
  needed to isolate one write statement's exception handling.
- Any change to the `createWithGeneratedCode` / `createWithCustomCode` insert-then-catch pattern —
  unrelated code paths, not touched.
- Making the click write asynchronous (`@Async` or otherwise moving it off the request thread) —
  considered and rejected as scope creep beyond the actual finding; re-derived in
  `technical-design.md` Section 4, not merely asserted here.

---

## 2. Endpoint Contract — `GET /{code}` (no new endpoint, existing endpoint's failure-path contract fixed)

**Request:** unchanged — `GET /{code}` (`LinkController.redirect`, line 95).

**Success — `302 Found`, unchanged in every case, including the newly-fixed one:**
```
302 Found
Location: <link.getTargetUrl()>
```
| Scenario | Status | Click recorded? | Trace |
|---|---|---|---|
| Code valid, not expired, click write succeeds (today's only "success" case) | `302 Found` | Yes | C5 |
| Code valid, not expired, click write throws a `RuntimeException` (**newly fixed**) | `302 Found` (unchanged shape) | **No** — logged, not recorded, not retried | C1, C2 |
| Code unknown | `404 CODE_NOT_FOUND` (unchanged) | N/A — never reaches the write | C4 |
| Code expired | `404 CODE_NOT_FOUND` (unchanged) | N/A — never reaches the write | C4 |

**No new `code` value, no new HTTP status, no new response field.** This fix changes zero entries in
`ApiExceptionHandler`'s existing table (`GET /{code}`'s only mapped exception is
`LinkUnavailableException` → `404 CODE_NOT_FOUND`, unchanged) — the whole point of C1 is that the
click-write failure no longer reaches `ApiExceptionHandler` at all, so no new handler or code is
needed for it. It is fully absorbed inside `LinkService`, observable only via the log (C2).

---

## 3. Design Decision: Isolate the Write Statement, Not the Method

**Decision:** the fix wraps only the click-write step (construct + persist the `ClickEventEntity`)
in a try/catch. It does **not** wrap the lookup (`shortLinkRepository.findByCode`, line 141) or the
expiry check (line 144-146) — both of those keep throwing `LinkUnavailableException` exactly as
today, uncaught by this fix's try/catch, because the try/catch's scope starts *after* both checks
have already passed.

**Why this is the right boundary, not an arbitrary one:** the finding (`prd-v0.md` Section 2.4) is
specifically that the *click write* is the one statement in this transaction with no exception
handling — the lookup and expiry check already have well-defined, tested failure behavior
(`LinkUnavailableException` → `404`, per C4/AC01-AC25 of `url-shortener-core`). Widening the
try/catch to cover the lookup would risk exactly the regression C4 forbids: a `catch
(RuntimeException)` placed around the whole method would also catch `LinkUnavailableException`
(it's a `RuntimeException`), silently turning an intended `404` into a swallowed no-op that returns
`link` — except `link` wouldn't even exist yet at that point, since the exception is thrown from
inside the `orElseThrow`. So the narrow boundary isn't just "minimal" as a style preference — it is
what keeps C4 true at all. See `technical-design.md` Section 1 for the exact code shape.

---

## 4. Traceability Matrix

| Contract clause | Mechanism | Section |
|---|---|---|
| C1 (redirect survives click-write failure) | Try/catch scoped to the write statement only, inside `redirectAndRecordClick` | 3, technical-design.md Section 1 |
| C2 (diagnosable log) | `log.error` with `code`, `link.getId()`, `now`, and the causing exception | 1, technical-design.md Section 1 |
| C3 (no response-shape/status change) | Zero edits to `LinkController.redirect()`'s response-building lines; zero new `ApiExceptionHandler` entries | 2 |
| C4 (`LinkUnavailableException` still propagates) | Try/catch scope excludes the lookup and expiry-check lines structurally | 3 |
| C5 (normal path unchanged) | Write still executes and still persists when nothing fails; return value unchanged | 1, 2 |
