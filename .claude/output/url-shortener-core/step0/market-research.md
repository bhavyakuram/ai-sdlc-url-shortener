---
agent: market-research
input: step0/concept.md
---

# Market Research: URL Shortener Service

## Technology Landscape
URL shorteners are a well-understood problem space (bit.ly, TinyURL,
Rebrandly, is.gd). The core mechanics are settled industry practice:

- **Code generation**: base62 (a-zA-Z0-9) encoding of an auto-increment
  id, or a random-token approach with a uniqueness check/retry. Base62
  of a sequential id is simpler to reason about for collision-safety
  and is what this project will use (see `technical-design`, STEP-3).
- **Redirect mechanism**: HTTP 301 (permanent) vs 302 (temporary).
  Comparable products default to 302 so that click analytics keep
  working (a 301 gets cached by browsers/CDNs, which silently kills
  server-side click tracking) — this project should use 302 for the
  same reason, given analytics is a Must-have.
- **Persistence**: comparable products use a key-value or relational
  store keyed by short code; both fit `java-spring`'s declared
  `data:relational` capability.

## Similar-Product Comparison
| Product | Custom alias | Analytics | Expiry |
|---|---|---|---|
| Bitly | Yes (paid tiers) | Rich (clicks, geo, device) | No default expiry |
| TinyURL | Yes | Basic | No default expiry |
| is.gd | Yes | Minimal | No expiry |

This project's proposed scope (custom alias + click-event log +
default TTL) sits reasonably in the middle — closer to Bitly's
feature shape than TinyURL/is.gd, but intentionally lighter (no
geo/device fingerprinting), consistent with idea.md's stated scope.

## Stack-Fit Assessment: java-spring
- Spring Boot's `@RestController` + Spring Data JPA fits the
  api/service/data layering directly; no architectural friction.
- Collision-safety on short-code creation is naturally handled by a
  unique DB constraint on `short_code` + retry-on-conflict at the
  service layer — idiomatic Spring Data pattern.
- Rate limiting (Could-have) has a standard idiomatic option
  (Bucket4j or a simple in-memory token bucket) if pursued later;
  not required for the Must-have set.
- No concerns that would block proceeding with `java-spring` for this
  feature set.
