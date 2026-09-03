# STEP-2 Specification: UX Flow (API Interaction Sequences) — url-shortener-core

**Phase:** STEP-2 · **Agent:** `ux-design` (skill: `skills/step2/ux-design/SKILL.md`)
**Stack:** java-spring · **Role:** greenfield · **Mode:** agentic · **Platform:** none
**Inputs read:** `step2/feature-spec.md` (this run's Part 1, above), `inputs/url-shortener-core/figma/`

**Figma check:** `mcp:figma` is declared optional in this agent's input contract. Per
`rules/mcp-convention.md`, this project's enabled MCP family set is code-graph only —
Jira/Figma/Playwright configs are scaffolded but inactive, and confirmed directly:
```
$ find .claude/inputs/url-shortener-core/figma -type f
find: .claude/inputs/url-shortener-core/figma: No such file or directory
```
No Figma export exists for this feature (expected — `java-spring` has no frontend layer per
`stacks/java-spring/stack-manifest.md`'s `layers: [api, service, data]`, no `ui` layer). **"UX" in
this document means API interaction sequences** — the request/response flow a client (browser,
curl, another service) follows — not UI screens or mockups. There is nothing to design against
visually; the "interaction" is entirely HTTP request/response.

---

## Sequence 1: Create -> Redirect -> Stats (happy path, end to end)

This is the core lifecycle of a link: an anonymous caller creates it, a link clicker follows it,
and the operator (or the creator) later checks its stats.

```
Link Creator                 Service                              Data Layer
     |                          |                                      |
     | 1. POST /api/v1/links    |                                      |
     |    {url: "https://..."} |                                      |
     |------------------------->|                                      |
     |                          | 2. validate url (scheme/length)      |
     |                          |    -- FR-3, feature-spec.md 3.1      |
     |                          |------------------------------------->|
     |                          | 3. generate 7-char base62 code       |
     |                          | 4. INSERT (unique constraint)        |
     |                          |------------------------------------->|
     |                          |<---- OK (no collision) --------------|
     |                          |    [on collision: retry step 3-4,    |
     |                          |     transparent to caller — FR-4]    |
     | 5. 201 Created           |                                      |
     |    {shortCode, shortUrl, |                                      |
     |     longUrl, createdAt,  |                                      |
     |     expiresAt}           |                                      |
     |<-------------------------|                                      |
     |                          |                                      |

Link Clicker                 Service                              Data Layer
     |                          |                                      |
     | 6. GET /{code}           |                                      |
     |------------------------->|                                      |
     |                          | 7. rate-limit check (IP, code)       |
     |                          |    -- Bucket4j, FR-9                 |
     |                          | 8. lookup code, check expiresAt      |
     |                          |------------------------------------->|
     |                          |<---- row found, not expired ---------|
     |                          | 9. record ClickEvent (timestamp,     |
     |                          |    referrer, geo-IP lookup — fails   |
     |                          |    soft if db unavailable, R-7)      |
     |                          |------------------------------------->|
     | 10. 302 Found            |                                      |
     |     Location: longUrl    |                                      |
     |<-------------------------|                                      |
     | 11. browser follows      |                                      |
     |     Location itself      |                                      |
     |     (service's involve-  |                                      |
     |     ment ends at step 10)|                                      |

Service Operator             Service                              Data Layer
     |                          |                                      |
     | 12. GET /api/v1/links/   |                                      |
     |     {code}/stats         |                                      |
     |------------------------->|                                      |
     |                          | 13. lookup code (existence only —    |
     |                          |     expiry NOT checked, feature-     |
     |                          |     spec.md Section 3.3)             |
     |                          |------------------------------------->|
     |                          |<---- row found -----------------------|
     |                          | 14. aggregate ClickEvents:            |
     |                          |     total / by-day / by-country      |
     |                          |------------------------------------->|
     |                          |<---- aggregates -----------------------|
     | 15. 200 OK               |                                      |
     |     {totalClicks,        |                                      |
     |      clicksByDay,        |                                      |
     |      clicksByCountry}    |                                      |
     |<-------------------------|                                      |
```

**Notes on this sequence:**
- Steps 3-4 are the FR-4/R-2 insert-then-catch collision loop — from the caller's point of view
  this is invisible; only the final `201` or (retry-exhausted, pathological) `503` is observed.
- Step 11 is explicitly annotated because it's a common point of confusion: the service's role in
  a redirect ends the instant it returns the `302` — it does not proxy or fetch the destination
  itself. This matters for the abuse-resilience NFR (the service's own resource usage per redirect
  is O(1) — one DB read, one DB write for the click event — regardless of what the destination URL
  does).
- Step 9's geo-IP lookup is drawn as part of the redirect's synchronous path, but per
  `feature-spec.md` Section 3.2 and `risk-register.md` R-7's mitigation, its failure must not
  block or delay step 10 — a stale/missing `.mmdb` degrades to `country: "unknown"` in the
  eventual stats view (step 15), never a failed redirect.
- Step 13 deliberately does *not* re-check `expiresAt` — this is the Section 3.3 decision that
  stats remain queryable for an expired link, drawn explicitly here so the sequence doesn't imply
  the same expiry gate applies to both endpoints.

---

## Sequence 2: Rate-Limit-Exceeded

A single link clicker (or a script) hammering the same short code from the same source IP faster
than FR-9's 100 req/min ceiling.

```
Link Clicker                 Service (Bucket4j token bucket, keyed on (IP, code))
     |                          |
     | Request 1..100           |
     | GET /{code}               (each request consumes 1 token)
     |------------------------->|
     |  302 Found (Location)     (bucket has tokens remaining each time)
     |<-------------------------|
     |  X-RateLimit-Remaining: 99, 98, ... 0  (decrementing header on each response)
     |
     | Request 101               (bucket empty — no tokens left in the 60s window)
     | GET /{code}
     |------------------------->|
     |                          | rate-limit check: 0 tokens available
     |                          | -- request is REJECTED before any DB lookup or
     |                          |    click-recording happens (fail fast, cheap)
     | 429 Too Many Requests     |
     |    Retry-After: 37        |
     |    X-RateLimit-Limit: 100 |
     |    X-RateLimit-Remaining: 0
     |    {code: "RATE_LIMITED"} |
     |<-------------------------|
     |
     | Request 102..N (within the same window)
     | GET /{code}
     |------------------------->|
     |  429 (same as above, no 5xx — NFR "no 5xx under 10x-threshold flood")
     |<-------------------------|
     |
     |  ... 60s window rolls forward, bucket refills ...
     |
     | Request N+1 (after window reset)
     | GET /{code}
     |------------------------->|
     |  302 Found                (bucket has tokens again — normal service resumes)
     |<-------------------------|
```

**Notes on this sequence:**
- The rate-limit check happens *before* the code-existence/expiry lookup (Sequence 1 step 8) and
  *before* click recording (step 9) — an over-limit request costs the service a bucket-map lookup
  only, not a DB round-trip. This is what keeps the "abuse resilience: no 5xx at 10x threshold"
  NFR achievable even under a real flood, since the expensive path (DB) is never reached once the
  bucket is empty.
- The key is `(source IP, short code)`, not IP alone or code alone — flooding one code from one IP
  does not rate-limit that same IP's requests to a *different* code, and does not rate-limit
  *other* IPs hitting the *same* code. This is drawn as a single-code, single-IP sequence because
  that is the unit FR-9 actually limits; a multi-key scenario is a repetition of this same sequence
  with a different bucket, not a new interaction pattern.
- This sequence applies only to `GET /{code}` (Section 6 of `feature-spec.md`) — `POST
  /api/v1/links` and `GET /api/v1/links/{code}/stats` have no rate limit in this spec and never
  return `429`.
