# STEP-2 Specification: Feature Spec — url-shortener-core

**Phase:** STEP-2 · **Agent:** `feature-spec` (skill: `skills/step2/feature-spec/SKILL.md`)
**Stack:** java-spring · **Role:** greenfield · **Mode:** agentic · **Platform:** none
**Inputs read (full):** `prework/prd-v0.md` (12 FRs, 6 NFRs, A1-A10 resolutions carried forward),
`step1/feasibility-report.md` (dependency picks: Bucket4j 8.19.0, MaxMind geoip2 5.2.0, H2 pinned
>= 2.2.220 file-mode), `step1/risk-register.md` (R-1..R-10, R-7/R-9 accepted as v1 trade-offs at Gate 1)

---

## 1. API Conventions

| Convention | Decision | Rationale / trace |
|---|---|---|
| Management base path | `/api/v1` (create, stats) | Standard versioned REST prefix — layer is `api` per `stacks/java-spring/stack-manifest.md`. |
| Redirect path | Root (`/{code}`, **no** `/api/v1` prefix) | A short link must itself be short — putting the redirect under `/api/v1/links/{code}` would defeat the product's purpose (concept.md's whole premise is a short, shareable URL). This is a spec-level design decision, not a new FR. |
| Request/response encoding | `application/json` for `POST /api/v1/links` and `GET /api/v1/links/{code}/stats`. The redirect endpoint (`GET /{code}`) returns no body — a `302` with a `Location` header only. | Standard REST practice; redirect bodies are ignored by browsers/clients anyway. |
| Timestamps | ISO-8601 UTC (`2026-09-02T14:03:11Z`) in every response field that carries a time. | Matches FR-7's 30-day expiry math and FR-10's clicks-by-day bucketing — unambiguous across client locales. |
| Field naming | `camelCase` in JSON bodies (`shortCode`, `longUrl`, `expiresAt`). | Idiomatic for a Spring Boot Jackson-serialized API; matches `stacks/java-spring/standards/naming.md` conventions applied at the JSON boundary. |
| Error envelope (all 4xx/5xx) | ```json\n{\n  "timestamp": "2026-09-02T14:03:11Z",\n  "status": 400,\n  "error": "Bad Request",\n  "code": "INVALID_URL_SCHEME",\n  "message": "URL must use http or https scheme"\n}\n``` | `code` is a stable machine-readable enum (below) distinct from the human-readable `message`, so a client can branch on `code` without string-matching. |
| Auth | None on any endpoint. | FR-6 (M6): "No login/account/session concept anywhere in the request path — this is a hard constraint carried into every downstream phase." |

### 1.1 Error `code` vocabulary
| `code` | HTTP status | Emitted by |
|---|---|---|
| `INVALID_URL_SCHEME` | 400 | `POST /api/v1/links` |
| `URL_TOO_LONG` | 400 | `POST /api/v1/links` |
| `URL_MALFORMED` | 400 | `POST /api/v1/links` |
| `INVALID_CUSTOM_CODE_SHAPE` | 400 | `POST /api/v1/links` |
| `RESERVED_CODE` | 400 | `POST /api/v1/links` |
| `CUSTOM_CODE_TAKEN` | 409 | `POST /api/v1/links` |
| `CODE_NOT_FOUND` | 404 | `GET /{code}`, `GET /api/v1/links/{code}/stats` |
| `LINK_EXPIRED` | *(internal reason only — never surfaced; see Section 4)* | `GET /{code}` |
| `RATE_LIMITED` | 429 | `GET /{code}` |
| `CODE_SPACE_EXHAUSTED` | 503 | `POST /api/v1/links` *(pathological case, see 3.1)* |

---

## 2. Endpoint Inventory

| # | Method | Path | Purpose | FR trace |
|---|---|---|---|---|
| 1 | `POST` | `/api/v1/links` | Create a short link from a long URL, optional custom code | FR-1, FR-3, FR-4, FR-5, FR-7, FR-8 |
| 2 | `GET` | `/{code}` | Redirect a short code to its long URL; records a click | FR-2, FR-7, FR-9, FR-10 |
| 3 | `GET` | `/api/v1/links/{code}/stats` | Return click analytics summary for a code | FR-10 |

Three endpoints total — this is the complete surface required by the Must+Should FRs in
`prework/prd-v0.md` Section 2.3. No additional CRUD (list/update/delete) endpoints exist:
FR-6/the "Out of Scope" list explicitly rules out ownership/edit/delete rights over a link.

---

## 3. Endpoint Specifications

### 3.1 `POST /api/v1/links` — Create Short Link

**Request**
```
POST /api/v1/links
Content-Type: application/json

{
  "url": "https://example.com/some/very/long/path?query=1",
  "customCode": "myLink1"     // optional
}
```

| Field | Type | Required | Validation | FR |
|---|---|---|---|---|
| `url` | string | yes | `http://` or `https://` scheme only; <= 2048 chars; must parse as a well-formed URL. Rejected (never silently altered/truncated) if any rule fails. | FR-3 |
| `customCode` | string | no | 3-32 chars, base62 alphabet (`[0-9A-Za-z]`) only; must not collide with a reserved top-level path segment (Section 5). | FR-8 |

**Success — `201 Created`**
```
201 Created
Location: /abc1234

{
  "shortCode": "abc1234",
  "shortUrl": "https://<host>/abc1234",
  "longUrl": "https://example.com/some/very/long/path?query=1",
  "createdAt": "2026-09-02T14:03:11Z",
  "expiresAt": "2026-10-02T14:03:11Z"
}
```
- `expiresAt` = `createdAt` + 30 days, fixed, no per-link override (FR-7 — no per-link override in v1).
- If `customCode` was supplied and available, `shortCode` echoes it verbatim; otherwise a
  server-generated 7-char base62 code is returned (FR-4).

**Error responses**
| Status | `code` | Condition |
|---|---|---|
| 400 | `INVALID_URL_SCHEME` | `url` scheme is not `http`/`https` (e.g. `javascript:`, `data:`, `file:`) |
| 400 | `URL_TOO_LONG` | `url` exceeds 2048 chars |
| 400 | `URL_MALFORMED` | `url` does not parse as a URL at all |
| 400 | `INVALID_CUSTOM_CODE_SHAPE` | `customCode` present but outside 3-32 chars / not base62 |
| 400 | `RESERVED_CODE` | `customCode` collides with a reserved path segment (Section 5) |
| 409 | `CUSTOM_CODE_TAKEN` | `customCode` present, valid shape, but already persisted for another link (first-come-first-served, FR-8) |
| 503 | `CODE_SPACE_EXHAUSTED` | Server-generated-code path only: insert-then-catch retry budget exhausted without finding a free code (see below) — pathological, not expected in practice at 7-char base62 keyspace size |

**Collision handling (FR-4, R-2 mitigation — insert-then-catch, never check-then-insert):**
For a server-generated code, the service attempts an `INSERT` with `shortCode` under a DB-level
`UNIQUE` constraint; on `DataIntegrityViolationException` it generates a new random code and
retries (bounded retry count). This is transparent to the client — collisions never surface as an
error response at this layer, they only cost internal latency. `CODE_SPACE_EXHAUSTED` (503) is the
one exception, reserved for the retry-budget-exhausted edge case, which `risk-register.md` R-2's
mitigation identifies as needing to be a hard DB constraint, not app-level locking, precisely so
this failure mode stays vanishingly rare rather than a race. For a **custom** code, there is no
retry — a collision is a direct, immediate `409 CUSTOM_CODE_TAKEN` (FR-8: "rejected, not mutated").

---

### 3.2 `GET /{code}` — Redirect

**Request**
```
GET /abc1234
```
No body, no headers required beyond standard HTTP.

**Success — `302 Found`**
```
302 Found
Location: https://example.com/some/very/long/path?query=1
```
- Side effect: exactly one `ClickEvent` is recorded (timestamp, coarse country via offline geo-IP
  lookup, referrer if the `Referer` header was present) — FR-10, NFR "Analytics completeness."
- If the geo-IP database is missing/stale (R-7), the lookup fails soft: the redirect still
  succeeds and the click is still recorded, with `country` omitted — this never blocks or fails
  the redirect itself, per `rules/coding-standards.md`'s No Silent Catches (log with context, do
  not crash the redirect path).

**Error responses**
| Status | Condition |
|---|---|
| 404 | `code` was never created, **or** `code` exists but is past its `expiresAt` (see Section 4 — deliberately the same status, same body shape, for both) |
| 429 | The (source-IP, `code`) pair has exceeded 100 requests in the trailing 60-second window (FR-9) — see Section 6 |

`404` body:
```json
{
  "timestamp": "2026-09-02T14:03:11Z",
  "status": 404,
  "error": "Not Found",
  "code": "CODE_NOT_FOUND",
  "message": "No active short link for this code"
}
```
Note the single `code: "CODE_NOT_FOUND"` value is used for *both* "never existed" and "expired" —
see Section 4 for why this is deliberate, not an oversight.

---

### 3.3 `GET /api/v1/links/{code}/stats` — Stats

**Request**
```
GET /api/v1/links/abc1234/stats
```

**Success — `200 OK`**
```json
{
  "shortCode": "abc1234",
  "longUrl": "https://example.com/some/very/long/path?query=1",
  "createdAt": "2026-09-02T14:03:11Z",
  "expiresAt": "2026-10-02T14:03:11Z",
  "totalClicks": 42,
  "clicksByDay": [
    {"date": "2026-09-02", "count": 10},
    {"date": "2026-09-03", "count": 32}
  ],
  "clicksByCountry": [
    {"country": "US", "count": 30},
    {"country": "unknown", "count": 12}
  ]
}
```
- `clicksByCountry` uses the literal value `"unknown"` for click events whose geo lookup failed
  soft (Section 3.2) — this is a real, expected bucket, not an error state.
- A link with zero clicks still returns `200` with `totalClicks: 0` and empty arrays — a link
  existing with no traffic yet is not a 404 condition (FR-10 "returns a summary," not "returns a
  summary if non-empty").
- **Stats remain queryable after a link expires.** Expiry (FR-7) only gates the *redirect* path
  (Section 3.2/4) — it says nothing about the *stats* path, and there is no FR requiring stats to
  disappear or 404 once a link is no longer redirectable. Treating them as still-queryable is the
  more conservative reading: it loses no information and matches FR-10's framing of stats as a
  historical summary, not a live-link-only view.

**Error responses**
| Status | `code` | Condition |
|---|---|---|
| 404 | `CODE_NOT_FOUND` | `code` was never created (this is the *only* 404 condition for this endpoint — an expired-but-once-valid code still returns 200, per the point above) |

---

## 4. Design Decision: 404, not 410, for Expired Codes

**Decision: `GET /{code}` on an expired code returns `404`, identical in status and body shape to
an unknown code. No `410 Gone` is used anywhere in this spec.**

**This is not a new decision being made at STEP-2 — it is already settled, Gate-0-approved input:**
- `prework/prd-v0.md` FR-2 (M2): "unknown/expired codes do not redirect."
- `prework/prd-v0.md` FR-7 (M7): "redirect on an expired code returns 404, not a stale 302" — the
  PRD's own phrasing explicitly names 404 and explicitly rules out treating expiry as distinct
  from "not found."
- Both trace to ambiguity resolution A1, `APPROVED` at Gate 0 per `_decisions.yaml` ("All 10
  ambiguity resolutions (A1-A10) ... accepted as proposed").

Per `rules/architecture.md` Write-Once Immutability, a Gate-0-approved decision is closed —
`feature-spec` does not have standing to relitigate it at STEP-2 without a new gate exception, and
none of the three sanctioned exceptions (standards waiver, primitive exclusion, contract-delta
amendment) apply here. So the operative question at this phase is narrower than "404 or 410" — it
is "does anything discovered since Gate 0 argue for reopening it as a contract-delta amendment,"
and the answer here is **no**, for reasons independent of the immutability rule as well:

1. **410 would leak information the PRD's own trade-offs already accept was better left unleaked.**
   `risk-register.md` R-9 (open-redirect/abuse exposure, accepted at Gate 1/Gate 2) already frames
   this as an anonymous, no-account, no-reputation-check service. A `410 Gone` response actively
   confirms "this code definitely existed and was valid at some point" — a strictly more useful
   signal to an attacker probing/enumerating codes than a uniform `404`, which reveals nothing
   about whether a code ever existed. Collapsing both cases to `404` is a small, free hardening
   that is consistent with, not additive to, the risk posture already accepted.
2. **No FR or NFR needs the distinction.** Nothing in `prd-v0.md` Section 2.3/2.4 asks a client to
   behave differently on "never existed" vs. "existed, now expired" — there is no retry-with-
   different-code UX, no "renew this link" feature (FR-7 explicitly: no per-link override in v1).
   Introducing 410 would add a status code with no consumer.
3. **Implementation simplicity matches the actual mechanism.** FR-7's mitigation (per
   `feasibility-report.md` Part 1) is a lazy check "before issuing 302" — the service does one
   query and one boolean check (`exists && !expired`), not two independently-branching lookups.
   Mapping that single boolean to a single status code is the natural fit; forcing a 410 branch
   would require distinguishing "row absent" from "row present but expired" as two different code
   paths purely to serve a status-code distinction nothing downstream consumes.

**If a future increment needs the distinction** (e.g., a "this link existed, want to recreate it?"
UX), that is a new FR requiring its own Gate-0-style ambiguity resolution, not something
`generator` should improvise from this spec.

---

## 5. Design Decision: Reserved-Code Collision (Custom Codes)

Because the redirect endpoint lives at the **root** path (Section 1, `/{code}`, not under
`/api/v1`), a custom code (FR-8, 3-32 chars) chosen by a caller could collide with a reserved
top-level segment the service also needs at the root or near-root — most notably `api` (the
management prefix itself), plus standard operational paths such as `actuator` (Spring Boot
Actuator, if enabled) and `health`.

**Decision:** `POST /api/v1/links` rejects (`400 RESERVED_CODE`, not mutated/renamed) any
`customCode` that exactly matches a reserved segment. The reserved list is a fixed set
(`api`, `actuator`, `health`, `favicon.ico`) checked at request-validation time, before the
uniqueness check. Server-generated codes are exempt from this check by construction: they are
7-char random base62 strings, and none of the reserved words are 7 characters of pure base62 that
a legitimate generation could ever be confused with routing-wise — routing collision is only
possible when a human deliberately types a short reserved word as a custom code.

This is a spec-level consequence of the "redirect lives at root" decision in Section 1, not a new
functional requirement — it is recorded here so `generator` doesn't have to independently discover
the routing collision while implementing `@RestController` path mappings.

---

## 6. Rate Limiting (FR-9)

| Property | Value |
|---|---|
| Scope | `GET /{code}` (redirect) only — not create, not stats (FR-9's text names only "the redirect endpoint") |
| Key | `(source IP, short code)` pair |
| Limit | 100 requests / rolling 60-second window |
| Mechanism | In-process token bucket, **Bucket4j 8.19.0** (`feasibility-report.md` Part 2 pick, chosen over hand-rolling specifically for its documented thread-safety — see `risk-register.md` R-4) |
| Storage bound | Bucket map must use a bounded/expiring backing store (R-8 mitigation) — not an unbounded `ConcurrentHashMap` |
| Over-limit response | `429 Too Many Requests`, no body required beyond the standard error envelope; `Retry-After` header (seconds until the window allows the next request) and `X-RateLimit-Limit`/`X-RateLimit-Remaining` headers on every redirect response (both under and over threshold) so clients can self-throttle |
| Failure mode | Must fail *safe*, i.e. return `429`, never throw/500, even under the NFR's stated 10x-threshold flood scenario |

```
429 Too Many Requests
Retry-After: 37
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 0

{
  "timestamp": "2026-09-02T14:03:11Z",
  "status": 429,
  "error": "Too Many Requests",
  "code": "RATE_LIMITED",
  "message": "Rate limit exceeded for this link; retry after the window resets"
}
```

---

## 7. Traceability Matrix

| FR | Endpoint(s) | Section |
|---|---|---|
| FR-1 | `POST /api/v1/links` | 3.1 |
| FR-2 | `GET /{code}` | 3.2, 4 |
| FR-3 | `POST /api/v1/links` | 3.1 |
| FR-4 | `POST /api/v1/links` | 3.1 |
| FR-5 | `POST /api/v1/links` (durability is a persistence-config concern, not a new endpoint behavior) | 3.1 |
| FR-6 | All three endpoints (absence of auth) | 1 |
| FR-7 | `GET /{code}` | 3.1 (`expiresAt`), 3.2, 4 |
| FR-8 | `POST /api/v1/links` | 3.1 |
| FR-9 | `GET /{code}` | 6 |
| FR-10 | `GET /{code}` (recording), `GET /api/v1/links/{code}/stats` (reporting) | 3.2, 3.3 |

All 10 Must+Should FRs are covered by exactly the 3 endpoints in Section 2. FR-11/FR-12 (Could
Have) are already folded into FR-10's shape per the PRD and require no additional endpoint.
