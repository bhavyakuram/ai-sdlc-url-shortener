# PRE-WORK: url-shortener-analytics-reliability (ambiguous/brownfield scenario)

**Stack:** java-spring · **Role (filed):** services-doc (posture=doc, layers_in_scope=[api, service], read-only)
**Mode:** agentic · **Platform:** none
**Raw input:** `.claude/inputs/url-shortener-analytics-reliability/supporting-docs/request.md`
**Agents combined in this artifact:** `triage`, `requirement-ingestion`, `posture-feasibility` (per
`rules/output-path.md`, prework writes to a single `prework/prd-v0.md`; each section below is one
agent's contribution). `role-feasibility-pass1` does **not** apply — it is scoped to `*-mod` postures
only (`rules/role-feasibility.md`), and this run is filed `doc`.

---

## 1. Triage Verdict

**Raw request, verbatim** (`inputs/url-shortener-analytics-reliability/supporting-docs/request.md`):
> "Filed posture: **doc** (audit only — just want a report, not new code).
>
> We think the click-analytics might not be totally reliable, especially if a link gets hit a lot at
> once. Can you check it out and tighten it up if needed? Not sure exactly what 'reliable' means here
> — just want confidence this isn't quietly losing data somewhere."

| Field | Value | Evidence |
|---|---|---|
| `feature_shape` | **incident-fix (suspected, conditional)** | The request is not "add a capability" (not `enhancement`), not a structural reshape for its own sake (not `refactor`), and real code already exists and runs (not `greenfield`). It is "we suspect an existing path is unreliable under load, go verify and correct it" — the canonical incident-fix shape, just phrased as a hypothesis ("might not be totally reliable") rather than a confirmed outage. |
| Does the request's own language imply conditional code changes, in tension with the filed `doc` posture? | **Yes, explicitly.** | "Can you check it out and **tighten it up if needed**" is a direct code-change instruction, conditioned on what the audit finds — not merely "explain how it works" or "write up the current behavior." The filed `doc` role's own manifest (`.claude/roles/services-doc/role-manifest.md`) states "Audit-only / documentation-improvement posture. **No code generation**," and lists `agents_skipped: [generator, test-generation]`. A role that structurally cannot invoke `generator` cannot honor "tighten it up if needed" if the audit concludes tightening is in fact needed. This is the exact tension `rules/posture-feasibility.md` is designed to catch (see Section 3). |
| `recommended_role` (filed fresh, independent of what was actually filed) | **services-mod** | `services-mod`'s manifest (`.claude/roles/services-mod/role-manifest.md`) is "Patch/enhance/fix existing service code," `layers_in_scope: [api, service]` (same layers `services-doc` already declares, just not read-only), `contract_posture: producer`. It is the minimal role that keeps both real outcomes of this request open: (a) audit finds nothing worth changing → no `generator` dispatch happens anyway, functionally identical cost to `doc`; (b) audit finds a real defect (Section 2 below shows one does exist) → the role can actually act on "if needed" without a mid-run role change, which `rules/mode-policy.md` and the Four Configuration Axes forbid. Filing `doc` for a request that contains its own conditional remediation clause under-scopes the run from the start. |
| `retry_budget` | **5** | Checked `.claude/run-history/_online-learning.yaml` verbatim (below) — it has exactly two matrix rows, `java-spring/greenfield` and `java-spring/services-mod`. There is **no** `java-spring/services-doc` row. Per `rules/retry-policy.md`, `adaptive-gate` may calibrate 3-8 only "based on historical pass rate for the stack+role combination" — with zero history for `services-doc`, nothing to calibrate against, so the framework default of 5 applies uncalibrated. Matches `_role-context.yaml`, which already records `retry_budget: 5`. |

**`_online-learning.yaml` verbatim (both rows present, neither is `services-doc`):**
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
    note: "1 run. Included a real BLOCKER + retry (STEP-4 compile error)."
  java-spring/services-mod:
    gate_decisions:
      gate_1: [GO]
      gate_2: [APPROVED]
      gate_3: [APPROVED]
      gate_6: [NO_EXCLUSIONS]
    pass_history: [PASS]
    reliability: {success_rate: 1.0, mttr_seconds: null, retries_consumed_total: 0}
    note: "1 run (url-shortener-bulk-shorten). Clean pass, zero retries. Different matrix row than greenfield -- adaptive-gate still has no cross-row history to generalize from, by design."
```

---

## 2. Requirement Ingestion — Real Click-Write Path, Read Fresh

### 2.1 The actual method (`service/LinkService.java:139-153`, verbatim)
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
    clickEventRepository.save(click);

    return link;
}
```
Called from `api/LinkController.java:98` (`GET /{code}`), which builds the 302 redirect response from
the returned `link` — **the same method call that records the click also produces the value the
redirect response is built from.** There is no separate "record click" step outside this transaction.

### 2.2 Is there a try/catch around the click write? **No.**
`clickEventRepository.save(click)` (line 150) is not wrapped in any try/catch. Compare this to the
**two other** writes in this same file, both of which *are* guarded:
- `createWithGeneratedCode` (lines 88-114): two `saveAndFlush` calls, each in its own
  `try { ... } catch (DataIntegrityViolationException ex) { throw new CodeSpaceExhaustedException(...); }`.
- `createWithCustomCode` (lines 117-127): one `saveAndFlush` in
  `try { ... } catch (DataIntegrityViolationException ex) { throw new CustomCodeTakenException(...); }`.

Both of those exist specifically because `short_link.code` has a `UNIQUE` constraint (`schema.sql`
line 8: `code VARCHAR(32) NOT NULL UNIQUE`) that concurrent requests can legitimately collide on —
insert-then-catch is this codebase's own established pattern for turning a real concurrency race into
a clean, typed outcome (`rules/coding-standards.md` No Silent Catches; both catches rethrow a specific
exception, never swallow).

### 2.3 Does `click_event` have an equivalent collision surface? **No — checked the schema directly.**
`schema.sql` lines 14-22:
```sql
CREATE TABLE IF NOT EXISTS click_event (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  short_link_id BIGINT NOT NULL REFERENCES short_link(id),
  occurred_at TIMESTAMP NOT NULL,
  referrer VARCHAR(512) NULL,
  country VARCHAR(2) NULL
);

CREATE INDEX IF NOT EXISTS idx_click_event_short_link_id ON click_event(short_link_id);
```
No `UNIQUE` constraint anywhere on `click_event`. Every click gets its own auto-increment `id`, so two
concurrent clicks on the same `short_link_id` can never collide on a constraint the way two concurrent
custom-code creations can. **The specific race pattern this file already knows how to handle
(insert-then-catch on a unique-constraint violation) does not apply to the click-write path at all** —
whatever "unreliable... hit a lot at once" means here, it is not that.

### 2.4 So what *is* the actual concurrency risk? Traced the real failure path.
`redirectAndRecordClick` is `@Transactional` end-to-end: the read of `link`, the geo lookup, and the
`click` insert are one transaction, committed by the Spring AOP transaction interceptor after the
method body returns and *before* control returns to `LinkController.redirect()`. Confirmed
`GeoLookupService.lookupCountry` (`service/GeoLookupService.java:63-76`) never throws — its own Javadoc
states "Never throws," and both its constructor and lookup method catch broadly
(`IOException | GeoIp2Exception | RuntimeException`) and degrade to `null`/"unknown" — so geo lookup is
not a risk factor here.

That leaves `clickEventRepository.save(click)` as the one statement in this transaction with no
explicit exception handling. If it throws for any reason (e.g. a JDBC-level failure — lock timeout,
connection-pool exhaustion under a burst of concurrent requests to the same popular code, since
`application.yml` configures no explicit HikariCP pool-size override and relies on the framework
default; H2 file-mode write contention under `AUTO_SERVER=TRUE`), the exception propagates:
1. out of `redirectAndRecordClick` (uncaught, so the `@Transactional` interceptor rolls back the whole
   transaction — the click row **and** the fact that `link` was ever read are both discarded),
2. out of `LinkController.redirect()` (also uncaught — no local try/catch there either, confirmed by
   reading `api/LinkController.java:94-102`),
3. into `ApiExceptionHandler`'s catch-all (`api/ApiExceptionHandler.java:82-86`, verbatim):
   ```java
   @ExceptionHandler(Exception.class)
   public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
       log.error("Unhandled exception processing request", ex);
       return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred");
   }
   ```

**Concrete answer to "does it risk silent data loss?" — No, not as literally asked.** A click-write
failure today is *loud*, not quiet: it is logged via `log.error(...)` with the full exception, and it
surfaces as a `500 INTERNAL_ERROR` to the caller. There is no code path in which the click silently
vanishes while the redirect quietly succeeds — `save()` is flushed at transaction commit, which happens
before the controller method returns, so a failed write cannot "sneak past" a successful-looking
response.

**The real defect found by reading the code is a different, more concrete one: availability coupling.**
Because the click write shares a transaction with the read that produces the redirect target, a
transient failure writing analytics currently takes the user-facing redirect down with it — on a
popular link getting hit hard concurrently by many distinct source IPs (`RateLimitInterceptor` throttles
per `(IP, code)` bucket at 100 req/min per `LinkController.java`'s own Javadoc, so it does **not** bound
total concurrent traffic to one code from many different IPs — e.g. a link going viral), a DB hiccup on
the analytics side would degrade the primary redirect path, not just analytics. That is the opposite of
what "reliable" implies for click-analytics as a supporting feature: today, click-write health is a
single point of failure for the thing users actually came for (being redirected).

### 2.5 Normalized, testable interpretations of "unreliable... hit a lot at once"
1. **Interpretation A — no silent loss for a successful redirect.** For every `302` response served,
   exactly one matching `click_event` row exists, and `getStats(code).totalClicks` reflects it.
   **Currently true by construction** (Section 2.4) — not at risk given the present code, verified
   structurally rather than assumed.
2. **Interpretation B — redirect availability should not depend on analytics-write success.** Under a
   concurrent burst, if the click insert hits a transient DB failure, the redirect should still succeed
   (its primary job) even if the click can't be recorded. **Currently false** — Section 2.4 shows the
   opposite is true today. This is the interpretation that best matches "tighten it up if needed": the
   fix is to isolate the click write (e.g. catch-and-log around `clickEventRepository.save(click)`,
   or move it out of the redirect's transaction boundary) so an analytics hiccup degrades only
   analytics completeness, never the redirect itself — the same fail-soft philosophy this codebase
   already applies to `GeoLookupService` (Section 2.4), just not yet applied to the click write itself.
3. **Interpretation C — raw throughput under sustained concurrent load.** Whether the service can
   sustain many simultaneous requests to one code without *any* exceptions at all is a capacity/tuning
   question (HikariCP pool sizing, H2 file-mode write throughput under `AUTO_SERVER=TRUE`) that this
   read of the code can raise as a hypothesis but not settle — there is no load-test harness anywhere
   in this repository (only `src/test/resources/application-test.yml` for ordinary unit/integration
   tests), so this would need an actual concurrent-request test to confirm empirically, not just
   inferred from source.

---

## 3. Posture Feasibility Verdict

**Filed posture: `doc`.** Applying `rules/posture-feasibility.md`'s own test: does the codebase
evidence match the posture filed, using the language `requirement-ingestion` actually found?

**Verdict: MISMATCH — flag at Gate 1.**

Reasoning, evidence-first (not a foregone conclusion — Section 2 above is a real, independent read of
the code, and it happens to land on a genuine finding):

1. **The raw request contains its own conditional remediation clause.** "Can you check it out and
   tighten it up **if needed**" is not documentation language ("explain," "describe," "write up") — it
   is a direct instruction to modify code, gated on what the audit turns up. This is close to a verbatim
   match for `rules/posture-feasibility.md`'s own worked example: "operator files `doc` posture, but
   `requirement-ingestion` finds language implying new code needs to be written."
2. **The filed role is structurally incapable of honoring that clause.** `.claude/roles/services-doc/role-manifest.md`
   states plainly: "Audit-only / documentation-improvement posture. **No code generation**," and lists
   `agents_skipped_if_ratified: [generator, test-generation]` (also recorded verbatim in this run's own
   `_role-context.yaml`). If Gate 1 ratifies `doc` as filed, "tighten it up if needed" cannot be
   fulfilled even if needed — the pipeline has no generator dispatch available in this lane.
3. **"If needed" is no longer hypothetical — Section 2 found a real, concrete reliability defect.** This
   is the part that turns a *process* mismatch (conditional language vs. an audit-only role) into a
   *substantive* one: the click-write path really does have an availability-coupling problem under
   concurrent load (Section 2.4), and "reliable... especially if a link gets hit a lot at once" was, in
   fact, pointing at something real, not a false alarm. A `doc`-only run can describe this finding but
   cannot act on it.
4. **This is not a case where `doc` would obviously be right anyway.** If the audit had found nothing
   (Interpretation A held and no coupling defect existed), `doc` would have been a fine, minimal-cost
   fit — the mismatch would have been theoretical only. That is not what happened here.

Per `rules/posture-feasibility.md` Outcome: **this does not auto-fail the run.** It surfaces as a
flagged warning at Gate 1 with the standard options:
- **RATIFY** — proceed as filed `doc`: this PRD (the report) becomes the deliverable, the availability
  coupling in Section 2.4 is documented but left unfixed, and a *separate* future run (filed `mod`)
  would be needed to actually "tighten it up."
- **EXPAND** — widen to `services-mod` (matching Section 1's independent triage recommendation): same
  `layers_in_scope: [api, service]`, `generator` becomes available, and the fix identified in Section
  2.5 Interpretation B can be implemented in this same run.
- **NARROW** — not applicable; there is no smaller role than `doc` to narrow to.
- **NO-GO** — not warranted; nothing here blocks the work, it is a scope question, not a feasibility
  blocker.

---

## 4. Summary for Gate 1

| Item | Resolution |
|---|---|
| Feature shape | incident-fix (suspected, conditional on audit findings) |
| Role as filed | `services-doc` — **flagged**, see Posture Feasibility |
| Role recommended if filed fresh | `services-mod` (Section 1) |
| Posture feasibility | **MISMATCH** — flagged at Gate 1 for RATIFY/EXPAND/NARROW/NO-GO (Section 3) |
| Retry budget | 5 (no `java-spring/services-doc` history in `_online-learning.yaml` — Section 1) |
| Does the click write risk *silent* data loss today? | **No** — a failure is logged (`log.error`) and surfaces as a loud `500 INTERNAL_ERROR`, not a silently dropped row (Section 2.4) |
| What's the real defect, if any? | **Availability coupling**: the click-event write shares one `@Transactional` boundary with the redirect read, so a transient analytics-write failure currently fails the user-facing redirect too, not just the analytics record (Section 2.4) |
| Concurrency collision surface | `click_event` has **no unique constraint** (unlike `short_link.code`) — the insert-then-catch pattern already used elsewhere in `LinkService.java` for `DataIntegrityViolationException` does not apply here; the risk is resource contention/timeout under burst load, not a constraint race (Section 2.3) |
| Recommended fix, if EXPAND is chosen | Isolate the click write (catch-and-log around `clickEventRepository.save(click)`, and/or move it out of the redirect's transaction boundary) so a click-write failure degrades analytics completeness only, never the redirect itself — same fail-soft philosophy this codebase already applies to `GeoLookupService` (Section 2.5, Interpretation B) |
