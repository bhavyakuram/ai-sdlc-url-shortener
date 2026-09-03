# STEP-2 Specification: Acceptance Criteria — url-shortener-core

**Phase:** STEP-2 · **Agent:** `acceptance-criteria` (skill: `skills/step2/acceptance-criteria/SKILL.md`)
**Stack:** java-spring · **Role:** greenfield · **Mode:** agentic · **Platform:** none
**Inputs read:** `step2/feature-spec.md` (endpoint contracts, error-code vocabulary, Sections 4-6
design decisions), `step2/ux-flow.md` (Sequences 1-2)

## Method
Every AC below cites the `feature-spec.md` section and FR it verifies, per `rules/testing.md`
("Every AC needs a test" — `evaluator` at STEP-5 cross-references these AC ids against test names/
tags). Coverage target, per the task brief: happy path + failure modes (invalid URL, collision,
expired, unknown code, rate-limited, concurrent-creation collision-safety) for every endpoint.

---

## `POST /api/v1/links` — Create Short Link

### AC01 — Happy path: generated code
**GIVEN** no `customCode` is supplied
**WHEN** the caller `POST`s `{"url": "https://example.com/a/b/c"}` to `/api/v1/links`
**THEN** the response is `201 Created` with a `shortCode` that is a 7-character base62 string, a
`shortUrl`, the echoed `longUrl`, `createdAt` set to now, and `expiresAt` set to exactly `createdAt`
+ 30 days
*(feature-spec.md 3.1; FR-1, FR-4, FR-7)*

### AC02 — Happy path: custom code
**GIVEN** `customCode: "myLink1"` is supplied and not already taken
**WHEN** the caller `POST`s `{"url": "https://example.com", "customCode": "myLink1"}`
**THEN** the response is `201 Created` with `shortCode` equal to `"myLink1"` verbatim (not
mutated), and the record is persisted under that exact code
*(feature-spec.md 3.1; FR-8)*

### AC03 — Invalid URL: disallowed scheme
**GIVEN** `url: "javascript:alert(1)"`
**WHEN** the caller `POST`s to `/api/v1/links`
**THEN** the response is `400 Bad Request` with `code: "INVALID_URL_SCHEME"`, and no record is
persisted
*(feature-spec.md 3.1; FR-3)*

### AC04 — Invalid URL: exceeds length limit
**GIVEN** `url` is a well-formed `https://` URL of 2049+ characters
**WHEN** the caller `POST`s to `/api/v1/links`
**THEN** the response is `400 Bad Request` with `code: "URL_TOO_LONG"`, and no record is persisted
*(feature-spec.md 3.1; FR-3)*

### AC05 — Invalid URL: malformed
**GIVEN** `url: "ht!tp://not a url"`
**WHEN** the caller `POST`s to `/api/v1/links`
**THEN** the response is `400 Bad Request` with `code: "URL_MALFORMED"`, and no record is
persisted
*(feature-spec.md 3.1; FR-3)*

### AC06 — Invalid custom code shape
**GIVEN** `customCode: "ab"` (2 chars, below the 3-char minimum)
**WHEN** the caller `POST`s `{"url": "https://example.com", "customCode": "ab"}`
**THEN** the response is `400 Bad Request` with `code: "INVALID_CUSTOM_CODE_SHAPE"`, and no record
is persisted
*(feature-spec.md 3.1; FR-8)*

### AC07 — Custom code collision
**GIVEN** a link already exists with `shortCode: "taken01"`
**WHEN** a second caller `POST`s `{"url": "https://other.example.com", "customCode": "taken01"}`
**THEN** the response is `409 Conflict` with `code: "CUSTOM_CODE_TAKEN"`, and the pre-existing
`taken01` record is unchanged (still points at its original `longUrl`)
*(feature-spec.md 3.1; FR-8, first-come-first-served)*

### AC08 — Reserved-code rejection
**GIVEN** `customCode: "api"` (a reserved top-level path segment)
**WHEN** the caller `POST`s `{"url": "https://example.com", "customCode": "api"}`
**THEN** the response is `400 Bad Request` with `code: "RESERVED_CODE"`, and no record is
persisted
*(feature-spec.md Section 5; routing-collision design decision)*

### AC09 — Concurrent creation, collision-safety: custom code
**GIVEN** no link with `shortCode: "raceCode"` exists yet
**WHEN** two `POST` requests with identical `{"url": ..., "customCode": "raceCode"}` are submitted
concurrently (no ordering guarantee between them)
**THEN** exactly one request receives `201 Created`, the other receives `409 Conflict
(CUSTOM_CODE_TAKEN)`, and exactly one row for `raceCode` is ever persisted — never two, and never
zero
*(feature-spec.md 3.1; FR-4/R-2 mitigation — DB unique constraint, not check-then-insert)*

### AC10 — Concurrent creation, collision-safety: generated codes
**GIVEN** N concurrent `POST` requests with distinct `url` values and no `customCode`
**WHEN** all N requests are submitted concurrently
**THEN** every request receives `201 Created`, and every returned `shortCode` is unique — no two
persisted rows ever share a `shortCode`, even when the underlying random generator produces the
same candidate code for two in-flight requests (the insert-then-catch retry resolves it before
either client sees an error)
*(feature-spec.md 3.1; FR-4, NFR "Collision rate: 0 duplicate active codes ever persisted, under
concurrency")*

---

## `GET /{code}` — Redirect

### AC11 — Happy path: valid, non-expired code
**GIVEN** a link exists with `shortCode: "abc1234"`, `longUrl: "https://example.com/x"`, not
expired
**WHEN** the caller sends `GET /abc1234`
**THEN** the response is `302 Found` with `Location: https://example.com/x`
*(feature-spec.md 3.2; FR-2)*

### AC12 — Unknown code
**GIVEN** no link with `shortCode: "zzzzzzz"` has ever existed
**WHEN** the caller sends `GET /zzzzzzz`
**THEN** the response is `404 Not Found` with `code: "CODE_NOT_FOUND"`
*(feature-spec.md 3.2; FR-2)*

### AC13 — Expired code
**GIVEN** a link exists with `shortCode: "old0001"` and `expiresAt` in the past
**WHEN** the caller sends `GET /old0001`
**THEN** the response is `404 Not Found` with `code: "CODE_NOT_FOUND"` — status and body
byte-identical in shape to AC12's unknown-code response (no `410`, per `feature-spec.md` Section 4)
*(feature-spec.md 3.2, Section 4; FR-7)*

### AC14 — Successful redirect records exactly one click
**GIVEN** a link exists with `shortCode: "abc1234"`, not expired, currently 0 recorded clicks
**WHEN** the caller sends `GET /abc1234` with header `Referer: https://ref.example.com`
**THEN** the response is `302 Found`, and exactly one `ClickEvent` is persisted with a timestamp
of now and `referrer: "https://ref.example.com"`
*(feature-spec.md 3.2; FR-10, NFR "Analytics completeness: exactly one ClickEvent per successful
redirect")*

### AC15 — Within rate limit
**GIVEN** the (source IP, `abc1234`) bucket has tokens remaining (fewer than 100 requests in the
trailing 60s window)
**WHEN** the caller sends `GET /abc1234`
**THEN** the response is `302 Found` (not `429`), with response headers `X-RateLimit-Limit: 100`
and `X-RateLimit-Remaining` decremented by 1 from the prior value
*(feature-spec.md Section 6; FR-9)*

### AC16 — Rate limit exceeded
**GIVEN** the (source IP, `abc1234`) pair has already made 100 requests within the trailing 60s
window
**WHEN** the caller sends a 101st `GET /abc1234` within that same window
**THEN** the response is `429 Too Many Requests` with `code: "RATE_LIMITED"`, a `Retry-After`
header, and **no** `ClickEvent` is recorded for this rejected request
*(feature-spec.md Section 6; FR-9)*

### AC17 — Rate limit is scoped per (IP, code), not per IP alone
**GIVEN** source IP `X` has exhausted its rate limit against `shortCode: "abc1234"`
**WHEN** the same IP `X` sends `GET /def5678` (a different, non-expired code)
**THEN** the response is `302 Found` — the bucket for `(X, "def5678")` is independent and unaffected
*(feature-spec.md Section 6, `ux-flow.md` Sequence 2 notes; FR-9)*

### AC18 — Rate limit window reset
**GIVEN** source IP `X` was rate-limited against `abc1234` at time `T`
**WHEN** IP `X` sends `GET /abc1234` again at `T + 61s` (after the 60s window has rolled over)
**THEN** the response is `302 Found` (bucket has refilled), not `429`
*(feature-spec.md Section 6; FR-9)*

### AC19 — Abuse resilience under flood, no 5xx
**GIVEN** a single valid code is targeted by a sustained flood at 10x the FR-9 threshold (1000
req/min from one source IP)
**WHEN** all 1000 requests are sent within the window
**THEN** the first 100 responses are `302`, the remaining 900 are `429` — **zero** responses are
`5xx`
*(feature-spec.md Section 6; NFR "Abuse resilience: no 5xx on the redirect endpoint under a
single-link flood at 10x the FR-9 rate-limit threshold")*

### AC20 — Geo-IP lookup fails soft
**GIVEN** the geo-IP `.mmdb` database is unavailable or stale (R-7 scenario)
**WHEN** the caller sends `GET /abc1234` (valid, non-expired code)
**THEN** the response is still `302 Found` (redirect is unaffected), the `ClickEvent` is still
recorded, and its `country` value is `"unknown"` — no `5xx`, no dropped click event
*(feature-spec.md 3.2; risk-register.md R-7 mitigation, `rules/coding-standards.md` No Silent
Catches)*

---

## `GET /api/v1/links/{code}/stats` — Stats

### AC21 — Happy path: link with recorded clicks
**GIVEN** `shortCode: "abc1234"` has 42 recorded `ClickEvent`s across 2 days and 2 countries
**WHEN** the caller sends `GET /api/v1/links/abc1234/stats`
**THEN** the response is `200 OK` with `totalClicks: 42`, `clicksByDay` summing to 42 across its
entries, and `clicksByCountry` summing to 42 across its entries
*(feature-spec.md 3.3; FR-10)*

### AC22 — Zero-click link (not a 404 condition)
**GIVEN** `shortCode: "fresh01"` was just created, 0 clicks recorded
**WHEN** the caller sends `GET /api/v1/links/fresh01/stats`
**THEN** the response is `200 OK` with `totalClicks: 0`, `clicksByDay: []`, `clicksByCountry: []`
*(feature-spec.md 3.3; FR-10 — a link existing with no traffic is not "not found")*

### AC23 — Unknown code
**GIVEN** no link with `shortCode: "neverExisted"` has ever existed
**WHEN** the caller sends `GET /api/v1/links/neverExisted/stats`
**THEN** the response is `404 Not Found` with `code: "CODE_NOT_FOUND"`
*(feature-spec.md 3.3; FR-10)*

### AC24 — Expired code remains queryable
**GIVEN** `shortCode: "old0001"` is past its `expiresAt` and has 5 recorded clicks from before
expiry
**WHEN** the caller sends `GET /api/v1/links/old0001/stats`
**THEN** the response is `200 OK` with `totalClicks: 5` (**not** `404`) — expiry gates the
redirect endpoint only, per `feature-spec.md` Section 3.3
*(feature-spec.md 3.3; distinguishes this endpoint's expiry behavior from `GET /{code}`'s, AC13)*

### AC25 — Referrer omitted when absent
**GIVEN** a click on `shortCode: "abc1234"` arrives with no `Referer` header
**WHEN** the stats for `abc1234` are later queried via `GET /api/v1/links/abc1234/stats`
**THEN** the click is still counted in `totalClicks`/`clicksByDay`/`clicksByCountry`, with no
error or dropped event caused by the missing referrer
*(feature-spec.md 3.2; FR-10 "referrer if present")*

---

## Coverage Summary

| Endpoint | Happy path | Invalid input | Collision | Expired | Unknown code | Rate-limited | Concurrency |
|---|---|---|---|---|---|---|---|
| `POST /api/v1/links` | AC01, AC02 | AC03-AC06, AC08 | AC07 | — | — | — | AC09, AC10 |
| `GET /{code}` | AC11, AC14, AC15 | — | — | AC13 | AC12 | AC16-AC19 | (covered via AC09/AC10 upstream; AC17/AC18 exercise concurrency-adjacent timing) |
| `GET /.../stats` | AC21, AC22, AC25 | — | — | AC24 | AC23 | — | — |
| Cross-cutting | AC20 (geo-IP fail-soft, R-7) | | | | | | |

**Total: 25 acceptance criteria (AC01-AC25),** covering every endpoint's happy path and every
failure mode named in the task brief (invalid URL, collision, expired, unknown code, rate-limited,
concurrent-creation collision-safety), plus two risk-register-derived edge cases (R-7 geo-IP
fail-soft, reserved-code routing collision) surfaced during spec authoring rather than left
implicit.
