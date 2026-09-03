# Concept: url-shortener-core

**Phase:** STEP-0 · **Agent:** concept-refinement · **Stack:** java-spring
**Source:** `.claude/inputs/url-shortener-core/ideation/idea.md` (verbatim, read in full — 22 lines)

## 1. Named Entities

| Entity | Definition | Notes |
|---|---|---|
| `ShortLink` | The core aggregate: a mapping from a generated (or user-chosen) short code to a target long URL, plus its lifecycle metadata (created-at, expires-at, status). | api/service/data layers all touch this; data-layer owner is the `data` package per `stacks/java-spring/stack-manifest.md`. |
| `ShortCode` | The token that appears in the redirect path (`/{code}`). Either system-generated (random) or operator-supplied (custom alias). | Value object, not a separate table — see MVP-1 decision. |
| `ClickEvent` | One recorded visit to a short link's redirect endpoint: timestamp, coarse origin, referrer (if present). | Append-only; feeds analytics. |
| `RedirectRequest` | The inbound HTTP request hitting `GET /{code}`; the trigger for both the 3xx redirect response and (async or synchronous) `ClickEvent` recording. | api-layer concern. |
| `Submitter` | The anonymous actor who creates a `ShortLink` via `POST /links` (or equivalent). No account/identity is persisted for it — see Ambiguity A5. | Not a stored entity, just a request-time role. |

## 2. Personas

1. **Anonymous Link Creator** — pastes a long URL into the service (or calls the API directly), optionally requests a custom code, gets a short link back. Never logs in. Primary v1 user; idea.md line 16 makes this explicit ("No login/accounts needed for v1").
2. **Link Clicker** — anyone who clicks/visits the short link. Never interacts with the service directly beyond the redirect; their request is the thing being measured.
3. **Service Operator** — the person who deployed the shortener and wants to know it's healthy and not being abused (idea.md line 9-11: "hold up under real usage... don't fall over if a bad actor hammers one link"). Consumes the analytics/monitoring surface, not a named account either — no admin-auth layer exists in v1.

No "Analyst" or "Team" persona is proposed: idea.md never mentions multi-tenant ownership, dashboards, or exports, and inventing one would exceed what STEP-0 is licensed to assume ahead of STEP-2 spec work.

## 3. Ambiguities Found in idea.md and Their Normalization

idea.md explicitly says "figure out what's reasonable for a first version and flag anything that needs a decision" (line 21). Per that instruction, every open point below is flagged with a specific resolution and the reasoning behind it, rather than left vague for STEP-2 to rediscover.

### A1. Expiry rule: fixed TTL vs. configurable per-link (idea.md line 14-15, explicitly flagged)
**Ambiguity:** "we haven't nailed down the exact rule... not sure yet whether expiry should be a fixed TTL for everyone or configurable per-link."
**Decision:** Fixed default TTL (**30 days** from creation) applied to every link automatically, with **no per-link override exposed in v1**.
**Justification:** A configurable-per-link TTL requires new input surface (validation range, UI/API field, edge cases like "TTL=0" or "never expires") that idea.md's MVP shape does not ask for anywhere else — everything else in the source doc is deliberately minimal (no accounts, no dashboards). A uniform default satisfies "links probably shouldn't live forever" with the least new surface area, and is trivially upgradable to per-link later (`rules/data-layer.md` Migration Safety already requires new fields to be additive). Flagged as a **Should-Have refinement, not Must-Have**, for STEP-2 to confirm with the operator at Gate 1/2 — this is a filed proposal, not a locked spec decision.

### A2. Analytics depth: counter vs. full click log (idea.md line 19, explicitly flagged)
**Ambiguity:** "analytics could mean anything from 'just a counter' to 'full click log'."
**Decision:** Full **append-only click log** (one `ClickEvent` row per redirect: timestamp + coarse origin + referrer), not just a counter — but with the *reporting* surface limited to a simple per-link summary (total clicks, clicks-by-day, clicks-by-country) rather than a raw log export/UI.
**Justification:** idea.md line 7-8 asks for three specific facets — "clicks, when they happen, roughly where traffic is coming from" — a bare counter can answer only the first. Storing the raw event is cheap (one row per hit) and keeps every future aggregation (daily trend, geo breakdown) derivable without a schema change, satisfying `rules/data-layer.md`'s additive-first migration bias. Building a full analytics *UI* (dashboards, exports, date-range pickers) is explicitly deferred to Won't-Have — the stack has no frontend layer (see Section 4) and idea.md never asks for one.

### A3. "roughly where traffic is coming from" — geolocation mechanism (idea.md line 8)
**Ambiguity:** Not stated whether this means IP-based geolocation, referrer-based origin, or something else, nor at what granularity.
**Decision:** Coarse **country-level** geo, derived from the requester's IP at redirect time using a **local, offline lookup** (embedded country-level IP database), not a live third-party geo API call.
**Justification:** `rules/mcp-convention.md` / `reference/platform-catalog.md` state the only MCP integration actually enabled for this project is `code-graph` — there is no geo-IP service configured, and standing one up is out of scope for a 2-3 day assignment. "Roughly where" (idea.md's own wording) supports coarse country-level resolution rather than city/precise geolocation, which also sidesteps extra privacy-sensitive data collection the source doc never asked for.

### A4. Persistence durability vs. the stack's in-memory default (idea.md line 9 vs. `stacks/java-spring/stack-manifest.md`)
**Ambiguity:** idea.md is explicit — "don't lose data" — but the java-spring stack manifest and `rules/data-layer.md` name H2 **in-memory** as the prototype default, which loses all data on process restart. These two requirements conflict as stated.
**Decision:** Use H2 in **file-based persistent mode** (`jdbc:h2:file:...`), not the pure in-memory mode, for this feature. Still zero external infra (no separate DB server to stand up), but survives a JVM restart.
**Justification:** `rules/data-layer.md` frames the DB engine choice as a stack-manifest capability token (`data:relational`), not a hardcoded mode — file-mode H2 satisfies that same token while actually honoring "don't lose data." Flagging this explicitly rather than silently defaulting to pure in-memory, which would quietly violate a stated hard requirement.

### A5. Custom short code: what does "if it's free" mean, and what governs collisions without accounts? (idea.md line 12-13)
**Ambiguity:** "Would be nice if people could pick their own short code instead of a random one, if it's free." "Free" is not disambiguated between (a) *free of charge* or (b) *free/available*, i.e. not already taken.
**Decision:** Read as **(b) available** — the feature is "let me pick my own code if nobody else already has it," not a pricing/tier concept. No accounts exist (line 16), so custom-code allocation is strictly **first-come-first-served**, unauthenticated, with no ownership, edit, or reclaim rights attached to it in v1.
**Justification:** idea.md never mentions money, tiers, or billing anywhere else — reading "free" as monetary would introduce a payments/plan concept the rest of the document gives zero support for. "Available" is the reading consistent with the rest of the doc's anonymous, no-account shape.

### A6. Short-code alphabet, length, and custom-code constraints (not mentioned in idea.md at all)
**Ambiguity:** No length, character set, or format rule is given for either generated or custom codes.
**Decision:** Generated codes: **7-character base62** (`[A-Za-z0-9]`), collision-checked against the store before being returned (see A7). Custom codes: same alphabet, **3-32 characters**, rejected (not silently truncated/altered) if outside that range or containing other characters.
**Justification:** Base62 is the de-facto standard for this product category (see `market-research.md` comparables) — 7 chars gives ~3.5×10^12 codes, ample for a v1 with no accounts to bound abuse otherwise. An explicit, rejecting validation rule (rather than silent mutation) is required by `rules/security.md` Input Validation for any externally-reachable field.

### A7. Collision handling under concurrency (idea.md line 10, "don't let two requests collide on the same short code")
**Ambiguity:** No mechanism specified for how concurrent requests are kept from colliding.
**Decision:** The short code column carries a **database-level unique constraint** (data layer), and the service layer catches the resulting constraint-violation and **retries generation** (generated codes) or **returns a 409 Conflict** (custom codes) rather than relying on an application-level check-then-insert, which is race-prone.
**Justification:** A `SELECT` to check availability followed by a separate `INSERT` is a classic TOCTOU race under concurrent load; the unique constraint makes the database itself the single source of truth, satisfying "don't let two requests collide" deterministically rather than probabilistically. Consistent with `rules/architecture.md` Dependency Direction — the invariant lives in the `data` layer, not duplicated as a service-layer lock.

### A8. Rate limiting: mechanism and threshold for "a bad actor hammers one link" (idea.md line 11)
**Ambiguity:** No specific threshold, scope (per-IP? per-link? global?), or response behavior given.
**Decision:** Per-**(source-IP, short-code)** token-bucket limit — **100 requests/minute**, HTTP `429 Too Many Requests` beyond that — enforced at the api layer, tracked in-process (no external rate-limit store) for v1.
**Justification:** Scoping to the (IP, code) pair rather than globally protects the redirect capacity for a link under attack without punishing unrelated links or unrelated clients hitting the same shortener instance — the narrowest reading of idea.md's "hammers **one** link" wording. An in-process bucket avoids introducing a new infra dependency (e.g. Redis) that nothing else in this MVP needs.

### A9. Redirect HTTP status: 301 (permanent) vs. 302/307 (temporary)
**Ambiguity:** Not addressed in idea.md at all, but materially affects browser/proxy caching behavior once expiry (A1) exists.
**Decision:** **302 Found** (temporary redirect) for every short link, never 301.
**Justification:** A link that can expire (A1) or later be deleted must not be permanently cached by browsers/intermediary proxies the way a 301 response is; 302 keeps every redirect re-checked against the live store, which is also a prerequisite for the click log (A2) actually counting every visit rather than only the first per client.

### A10. Long-URL validation scope (idea.md line 5, "submits a long URL")
**Ambiguity:** No rule for what makes a submitted URL acceptable (scheme restriction, length cap, malformed-input handling).
**Decision:** Require `http://` or `https://` scheme, reject (400) anything else (including `javascript:`, `data:`, `file:` schemes), cap total length at **2048 characters**, reject malformed input rather than attempting to "fix" it.
**Justification:** `rules/security.md` Input Validation makes boundary validation on every externally-reachable input non-optional; explicitly excluding `javascript:`/`data:` schemes prevents the shortener being used as an open redirector for script-injection payloads — a concrete, well-known abuse pattern for this exact product category.

## 4. MVP Features (MoSCoW)

**No frontend layer exists for this stack/role** (`layers_in_scope: [api, service, data]` per `_role-context.yaml`) — every feature below is an API-level capability; no UI/UX feature is proposed anywhere in this list.

### Must Have
- **M1.** `POST` endpoint: submit a long URL, receive a generated short code + full short URL. (idea.md line 5)
- **M2.** `GET /{code}` redirect endpoint: resolves a valid, non-expired short code to its long URL via 302. (idea.md line 6; A9)
- **M3.** Long-URL boundary validation per A10 (scheme allow-list, length cap, reject malformed).
- **M4.** Collision-safe code generation/storage per A7 (DB unique constraint + retry/409).
- **M5.** Durable persistence (H2 file-mode per A4) — no data loss across a process restart.
- **M6.** Anonymous operation — no login/account/session concept anywhere in the request path. (idea.md line 16)
- **M7.** Fixed-TTL expiry per A1: every link expires 30 days after creation; expired codes 404 on redirect, not 302-to-nowhere.

### Should Have
- **S1.** Custom short code on creation, first-come-first-served, per A5/A6 validation rules.
- **S2.** Per-(IP, code) rate limiting on the redirect endpoint per A8.
- **S3.** Click analytics: full append-only `ClickEvent` log per A2 (count, timestamp, coarse country) with a simple per-link summary read endpoint (`GET /links/{code}/stats`).

### Could Have
- **C1.** Clicks-by-day trend in the stats response (derivable from the A2 log with no schema change — additive read-side aggregation only).
- **C2.** Referrer capture on `ClickEvent` (already-available HTTP header, zero new input surface).

### Won't Have (v1)
- **W1.** Accounts, ownership, or edit/delete rights over a link (idea.md line 16 explicitly rules this out; A5 depends on it).
- **W2.** Per-link configurable expiry / expiry-rule override (A1 — fixed default only for v1).
- **W3.** Any analytics dashboard/export UI (no frontend layer in scope at all; A2 limits v1 to a JSON summary endpoint).
- **W4.** Precise (city/lat-long) geolocation — only country-level per A3.
- **W5.** Custom domains / branded short-link hosts (never mentioned in idea.md).

## 5. Success Metrics (baseline for `_reliability-metrics.json` KPI rollup per `rules/greenfield-scaffold.md`)

| Metric | Target | Ties to |
|---|---|---|
| Redirect correctness | 100% of non-expired, valid codes resolve to their original long URL | M2 |
| Redirect latency | p95 < 50ms (in-process H2, no external network hop) | M2, non-functional per idea.md line 9 "hold up under real usage" |
| Collision rate under concurrent create | 0 duplicate active codes ever persisted | M4 / A7 |
| Data durability | 0 link records lost across a normal process restart | M5 / A4 |
| Abuse resilience | Redirect endpoint stays available (no 5xx) under a single-link flood at 10x the S2 rate-limit threshold | S2 / A8 |
| Analytics completeness | Every successful redirect produces exactly one `ClickEvent` row | S3 / A2 |

## 6. Explicit Non-Goals Carried Forward from idea.md
- No login/accounts in v1 (line 16) — carried into every downstream phase as a hard constraint, not just a v1 nicety, since removing it later is an additive change, not a breaking one.
- No UI is implied or should be inferred anywhere in this concept — the java-spring role for this run declares `layers_in_scope: [api, service, data]` only.
