---
agent: concept-refinement
input: .claude/inputs/url-shortener-core/ideation/idea.md
---

# Concept: URL Shortener Service

## Named Entities
| Entity | Description |
|---|---|
| `ShortLink` | A mapping from a short code to a target (long) URL. Has an optional custom alias, an optional expiry, and creation metadata. |
| `ClickEvent` | A single recorded visit to a `ShortLink`: timestamp, referrer (if present), coarse outcome (redirected / expired / not-found). |
| *(deferred)* `LinkOwner` | Not modeled in v1 — idea.md explicitly says no accounts/login for v1. |

## Personas
1. **Anonymous Link Creator** — submits a long URL, optionally requests
   a custom alias, gets back a short link. No account.
2. **Link Visitor** — clicks/opens a short link, expects a fast
   redirect to the original URL (or a clear expired/not-found response).
3. **Operator/Analyst** — wants to see how a given short link is
   performing (click count over time, at minimum).

## MVP Features (MoSCoW)

**Must have**
- Create a short link from a long URL (`POST /links`)
- Redirect a short code to its target URL (`GET /{code}`)
- Short codes are collision-safe under concurrent creation
- Basic click analytics: count + timestamp per click, retrievable per link

**Should have**
- Custom alias support, best-effort (if the requested alias is taken,
  return a clear conflict rather than silently substituting one)
- Link expiry — **flagged ambiguity below**

**Could have**
- Referrer capture on click (best-effort, not guaranteed)
- Basic abuse protection (rate limiting on link creation)

**Won't have (v1)**
- User accounts / authentication
- Custom domains
- Bulk import/export

## Flagged Ambiguities (from idea.md — normalized for STEP-2 to resolve formally)
1. **Expiry rule is unspecified.** idea.md says "some kind of expiry
   would be good... haven't nailed down the exact rule." **Proposed
   normalization for this concept**: default fixed TTL (90 days) applied
   at creation, *overridable per-link* at creation time (optional field,
   not required). This is a proposal, not a decision — confirm at Gate 0
   or defer explicitly to STEP-2 `feature-spec` / `acceptance-criteria`.
2. **Analytics depth is unspecified.** idea.md says "could mean anything
   from a counter to a full click log." **Proposed normalization**: a
   click *event log* (timestamp + best-effort referrer), with count
   derivable from it — richer than a bare counter, short of full
   geo/device fingerprinting (out of scope, no such requirement was
   stated).
3. **Custom alias behavior on collision** was not addressed in idea.md
   at all — proposed: reject with `409 Conflict`, do not silently
   fall back to a random code (a silent fallback could surprise a
   caller who specifically wanted that alias).

## Success Metrics (baseline for `rules/greenfield-scaffold.md` KPI tracking)
- Redirect P99 latency target: < 100ms (in-memory/local datastore)
- Zero short-code collisions under the collision-safety test in STEP-5
- 100% of Must-have ACs covered by tests (per `rules/testing.md`)
