# STEP-2 Specification: Acceptance Criteria — url-shortener-analytics-reliability

**Phase:** STEP-2 · **Agent:** `acceptance-criteria` (skill: `skills/step2/acceptance-criteria/SKILL.md`)
**Stack:** java-spring · **Role:** services-mod · **Mode:** agentic · **Platform:** none
**Inputs read:** `step2/feature-spec.md` (Sections 1-4)
**Numbering:** continues from `url-shortener-bulk-shorten/step2/acceptance-criteria.md`, which ends
at AC40 — this document begins at **AC41**, per `rules/architecture.md` Write-Once Immutability
(neither the core feature's AC01-AC25 nor the bulk-shorten feature's AC26-AC40 are renumbered or
reopened). No `ux-flow.md` exists for this feature (no new endpoint, no new UI-observable flow — see
`step2/feature-spec.md` Section 0/2) — both new ACs below are cited against `feature-spec.md` only.

## Method
Both ACs below cite the `feature-spec.md` contract clause (C1-C5) and section they verify, per
`rules/testing.md` ("Every AC needs a test" — `evaluator` at STEP-5 cross-references these AC ids
against test names/tags). Coverage target, per the task brief: the failure path (redirect survives
a click-write failure) and the normal path (click still recorded when nothing fails), as a positive
regression guard against the fix itself.

---

## `GET /{code}` — Redirect Availability Independent of Click-Write Success

### AC41 — Redirect succeeds even if the click-write throws
**GIVEN** a valid, non-expired short code `code`, and `clickEventRepository.save(...)` (or
`saveAndFlush(...)`, per the exact write call `technical-design.md` specifies) throws a
`RuntimeException` when invoked for this request (e.g. simulated via a mocked/spied
`ClickEventRepository` in a unit test, or an injected transient failure in an integration test)
**WHEN** the caller sends `GET /{code}`
**THEN** the response is still `302 Found` with `Location` set to the link's `targetUrl` — the
exact same status code and header the endpoint returns on a fully successful request — and no
exception propagates to `ApiExceptionHandler` (no `500 INTERNAL_ERROR`, no changed status); the
click-write failure is logged (`log.error`, verified via a log-capture assertion) with the short
`code`, the resolved link id, and the attempted timestamp present in the log record; **and** no
`ClickEventEntity` row exists for this request afterward (the failed write is not silently retried
or partially persisted)
*(feature-spec.md Section 1 C1/C2, Section 2 "click write throws" row, Section 3; prd-v0.md Section
2.5 Interpretation B)*

### AC42 — Normal path unchanged: click IS still recorded when nothing fails
**GIVEN** a valid, non-expired short code `code`, and no failure is injected anywhere in the request
(lookup, expiry check, geo lookup, and click write all succeed exactly as today)
**WHEN** the caller sends `GET /{code}`
**THEN** the response is `302 Found` with `Location` set to the link's `targetUrl` (byte-for-byte
the same response `url-shortener-core/step2/acceptance-criteria.md`'s redirect ACs already pin), and
exactly one new `ClickEventEntity` row exists afterward with this request's `shortLinkId`,
`occurredAt`, `referrer`, and `country` — immediately visible via
`GET /api/v1/links/{code}/stats`'s `totalClicks` count incrementing by exactly 1
*(feature-spec.md Section 1 C5, Section 2 "click write succeeds" row; regression guard against the
fix in AC41 — this is what proves the try/catch added for AC41 is scoped to the failure path only
and does not change behavior when there is no failure to isolate)*

---

## Coverage Summary

| Scenario | AC(s) |
|---|---|
| Click-write failure isolated from redirect (the fix) | AC41 |
| Normal path unchanged (regression guard) | AC42 |

**Total: 2 new acceptance criteria (AC41-AC42),** continuing `url-shortener-bulk-shorten`'s
AC26-AC40 without renumbering or reopening it. Note what is deliberately **not** a separate AC
here, per `feature-spec.md` Section 1 C4: `LinkUnavailableException` propagation (unknown/expired
code → `404 CODE_NOT_FOUND`) is not re-tested by a new AC in this document because it is an existing,
already-covered behavior (`url-shortener-core/step2/acceptance-criteria.md`'s redirect ACs) that
this fix does not touch or risk — `technical-design.md` Section 1's exact code boundary is what
keeps that true, and `evaluator` at STEP-5 should confirm those existing tests still pass unmodified
rather than expecting a new AC id for them.
