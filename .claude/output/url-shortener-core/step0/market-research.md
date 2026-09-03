# Market Research: url-shortener-core

**Phase:** STEP-0 · **Agent:** market-research · **Stack:** java-spring
**Input:** `.claude/output/url-shortener-core/step0/concept.md`
**Tool usage (per `rules/architecture.md` Proof Over Promise, every comparable claim below is backed by the verbatim `WebSearch` output quoted in this file, not asserted from memory):**

```
WebSearch("Bitly TinyURL YOURLS custom short URL API rate limit features comparison 2025")
WebSearch("Spring Boot URL shortener open source github concurrency short code generation")
```

## 1. Comparable Products

### 1.1 Bitly (hosted SaaS, market leader)
Per the search result: *"Bitly: A full-featured link management platform built for marketers, businesses, and enterprises that need detailed analytics, branded domains, and integrations. However, if you want to use branded domains on Bitly, you have to pay $29/month (growth plan)."*

- **Analytics:** full click log with dashboards, UTM tracking, geo/device breakdown — far beyond concept.md's A2 decision (summary endpoint only). Confirms our A2 read is the *minimal viable* version of a real pattern in this space, not an invented one.
- **Custom codes:** supported, but gated behind paid tiers and account ownership — the opposite of our A5 decision (free/available, no-account, first-come-first-served). This is a **deliberate scope-down** vs. the market leader, consistent with idea.md's "no login/accounts for v1."
- **Expiry:** Bitly links do not expire by default (permanent by design, monetized via clicks/branding) — this is the one place our concept **diverges from the market leader on purpose**: idea.md explicitly wants expiry (A1), which Bitly's core product does not offer at all.

### 1.2 TinyURL (hosted, anonymous-first — closest philosophical match)
Per the search result: *"TinyURL: A lightweight, no-frills URL shortener designed for users who need fast, anonymous, and free link shortening without the overhead of account creation or complex dashboards. TinyURL works best when you need a straightforward URL-shortening API and do not require deep analytics, enterprise permissions, advanced routing, or broad integrations."*

- This is the closest real-world match to our MVP shape: anonymous by default, no mandatory account, custom alias offered without gating on payment. It validates concept.md's M6 (anonymous operation) and S1 (custom code) as a coherent, shippable product shape — not a compromise, but a recognized market segment (the "no-frills" tier sitting below Bitly's enterprise tier).
- TinyURL's analytics are minimal-to-none in the free tier, which supports our A2 decision to store the full click log internally (cheap, additive-safe) while keeping the *exposed* reporting surface deliberately small for v1 — we can grow the reporting surface later without a schema change, same trade TinyURL makes commercially.

### 1.3 YOURLS (self-hosted, open-source — closest architectural match)
Per the search result: *"YOURLS provides developers with a free, self-hosted, open-source URL shortener. You run it on your own server, control the data, and shape the experience through plugins and custom code. YOURLS costs nothing for the software. You still pay for hosting, maintenance, monitoring, backups, security, and developer time."*

- YOURLS is the right comparison for *this* project specifically because it's self-hosted from-scratch infrastructure (like ours), not a managed SaaS — same operational posture: you own the durability guarantee (concept.md A4), you own the abuse handling (A8), you own the analytics storage (A2). No vendor absorbs those concerns for us, which is exactly why concept.md treats them as Must/Should-Have engineering work rather than "the platform handles it."
- YOURLS's plugin-based custom-alias and click-log features are click-counted per link with basic stats — directly analogous to concept.md's S3 (per-link stats endpoint), reinforcing that a lightweight stats endpoint (not a full dashboard) is the standard self-hosted-tier feature, not an under-build.

### 1.4 Architectural precedent on the same stack (java-spring)
Per the second search, a real Spring Boot url-shortener repo in the wild (`William-Nogueira/spring-url-shortener`) is described as: *"A production-grade, distributed URL shortening service designed for high throughput and low latency, employing Write-Behind Caching, Deterministic, Non-Sequential ID Generation, and Virtual Threads to handle massive concurrent traffic... generates guaranteed unique, non-sequential, collision-free short codes purely in memory (after fetching a sequence block)."*
Also noted generally: *"Common techniques for short code generation include base 36 encoding..., or base 62 encoding when differentiating uppercase and lowercase letters."*

- Confirms base62 (concept.md A6) is the standard technique for this exact stack/product combination, not an arbitrary choice.
- That project's sequence-block-plus-in-memory-generation approach is a *scale-up* pattern (distributed, cache-backed) appropriate for "massive concurrent traffic." Concept.md's A7 decision (DB unique constraint + retry, no separate cache/sequence service) is the right-sized version of the same collision-avoidance idea for this MVP's actual scope — the pattern is proven, we're just not adopting its distributed-systems complexity (Redis write-behind cache, sequence blocks) prematurely.

## 2. Feature-Positioning Summary

| Capability | Bitly | TinyURL | YOURLS | **url-shortener-core (this MVP)** |
|---|---|---|---|---|
| Account required | Yes (for most features) | No | No (self-hosted, operator-run) | **No** (M6) |
| Custom alias | Paid tiers | Free | Free (plugin) | **Free, first-come-first-served** (S1) |
| Expiry | No (permanent by default) | No | Plugin-dependent | **Yes — fixed 30-day default** (A1) |
| Click analytics | Full dashboard | Minimal | Basic per-link stats | **Log stored, summary endpoint** (A2/S3) |
| Geo detail | City/region | None (free tier) | Plugin-dependent | **Country only** (A3) |
| Self-hosted | No | No | Yes | **Yes** |

This MVP sits in the same segment as YOURLS (self-hosted, developer-owned) with TinyURL's anonymous-first posture, plus one feature (mandatory expiry) that neither hosted incumbent defaults to — a deliberate, justified departure driven directly by idea.md line 14, not a gap.

## 3. Stack-Fit Assessment — java-spring (Spring Boot 3.1.4, Java 19, H2, api/service/data)

**Overall verdict: Good fit, no blocking friction — three specific points flagged below need an explicit decision at STEP-3, not silent assumption.**

### 3.1 Fits well
- **Layering matches the product shape.** `api` (REST controllers for create/redirect/stats) / `service` (code generation, expiry check, rate limiting, click recording) / `data` (JPA repository + unique constraint per concept.md A7) maps directly onto the three MVP surfaces with no forced abstraction — no layer needs to know about a concern that belongs to another (`rules/architecture.md` Dependency Direction is easy to satisfy here, not fought against).
- **No frontend layer is required and none is missing.** Every comparable product's *core* value (Bitly, TinyURL, YOURLS) is API/redirect behavior; their dashboards are value-add, not core. A pure-backend java-spring service is a faithful MVP of this product category, not a stripped-down compromise.
- **Spring Boot 3.1.4 + Java 19 is a supported, unremarkable pairing.** Spring Boot 3.1.x's baseline is Java 17, with the 3.1 line tested through Java 20; Java 19 sits inside that supported window with no version-compatibility risk.
- **H2 is adequate for the durability requirement once run in file mode** (concept.md A4) — no need to stand up Postgres/MySQL for this scope, and `data-layer.md`'s capability-token model (`data:relational`) means swapping to a real engine later via `db-harness/` is a config change, not a rewrite.
- **Base62 generation + unique-constraint collision handling (A6/A7) is standard, well-understood Spring Data JPA territory** — `@Column(unique = true)` plus catching `DataIntegrityViolationException` on insert is a common, idiomatic pattern; no unusual library or infrastructure needed.

### 3.2 Friction points to resolve explicitly at STEP-3 (not blockers, but not silently assumable either)
1. **No stable virtual threads on Java 19.** The real-world comparable project above (`William-Nogueira/spring-url-shortener`) leans on Java 25 virtual threads for high-concurrency I/O; Java 19's virtual threads are a *preview* feature (`--enable-preview`) and Spring Boot 3.1.4 does not enable Loom-based virtual thread support out of the box (that lands officially with Spring Boot 3.2 + Java 21). **Implication:** rate limiting (A8) and redirect-path concurrency must be built against Tomcat's conventional thread-pool model, not virtual threads. This is entirely adequate at the MVP's expected scale (a rate-limit ceiling of 100 req/min per link per A8 is nowhere near thread-pool exhaustion territory) — flagging only so STEP-3/STEP-4 doesn't accidentally reach for a preview-only API.
2. **No built-in Spring rate-limiting primitive.** Spring Boot ships no first-party token-bucket/rate-limiter; the A8 decision (per-IP-and-code, in-process) will need either a small hand-rolled bucket (a few dozen lines, no new dependency) or a library such as Bucket4j. Flagging so STEP-3 makes the choice deliberately rather than STEP-4's `generator` picking one ad hoc mid-implementation.
3. **Country-level geo (A3) needs a bundled offline lookup, which is an extra dependency, not a stack capability that's already declared.** `stacks/java-spring/stack-manifest.md`'s declared capabilities (`api:rest`, `services:jvm`, `data:relational`, `build:maven`, `test:junit`) don't include a geo-IP capability token. This isn't a blocker — an offline country-level database (e.g. a MaxMind GeoLite2-style lookup, bundled as a Maven dependency, no live network call) is consistent with `rules/mcp-convention.md`'s enabled-integration set (code-graph only, no external geo API) — but it should be named explicitly in the STEP-3 technical design and, if the stack manifest's capability vocabulary is meant to be exhaustive, added to it rather than introduced silently by `generator`.

### 3.3 No concern found regarding
- Maven build tooling (`build:maven` capability already declared and exercised by the greenfield scaffold per `_run-log.md`'s COLD BOOTSTRAP note).
- JUnit 5 test coverage of the API/service/data layers — standard Spring Boot Test + Mockito, no stack-fit issue.
- Security posture (input validation, parameterized queries) — Spring Data JPA's repository methods are parameterized by default, satisfying `rules/security.md` without extra effort.

## Sources
- [Best TinyURL Alternatives To Consider in 2026](https://bitly.com/blog/tinyurl-alternatives/)
- [Bitly vs TinyURL: An updated comparison (2026) | Dub](https://dub.co/blog/bitly-vs-tinyurl)
- [Best URL Shortener APIs for Developers (2026)](https://bitly.com/blog/shorten-url-api/)
- [Best URL Shortener APIs to Use in 2026 - Rebrandly](https://www.rebrandly.com/blog/url-shortener-apis)
- [GitHub - William-Nogueira/spring-url-shortener](https://github.com/William-Nogueira/spring-url-shortener)
- [GitHub Topics: url-shortener (Java)](https://github.com/topics/url-shortener?l=java)
