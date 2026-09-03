# STEP-1: Dependency Audit — url-shortener-bulk-shorten

**Agent:** dependency-audit · **Input:** `prework/prd-v0.md`, `service-java-spring/pom.xml`
(read verbatim).

## Verdict: **NO NEW DEPENDENCY REQUIRED.**

## Evidence

Full `pom.xml` dependency list (verbatim):
```
org.springframework.boot:spring-boot-starter-web
org.springframework.boot:spring-boot-starter-validation
org.springframework.boot:spring-boot-starter-data-jpa
com.h2database:h2:2.2.224 (runtime)
com.bucket4j:bucket4j_jdk17-core:8.19.0
com.maxmind.geoip2:geoip2:5.2.0
org.springframework.boot:spring-boot-starter-test (test)
```

Everything the bulk-create endpoint needs is already present:
- **JSON binding for a nested `items[]` array** — `spring-boot-starter-web`'s bundled Jackson
  handles an array-of-objects request body the same as it already handles the flat
  `CreateLinkRequest` body; no new (de)serialization library needed.
- **Bean validation on batch-level shape** (`items` non-empty, `items.length <= 100`) —
  `spring-boot-starter-validation` (already a dependency) covers `@NotEmpty`/`@Size` annotations
  on a request record's `List<CreateLinkRequest> items` field, or the same checks can be done by
  hand in the new batch method exactly as `LinkService.validateUrl()` already hand-validates
  today — either path uses only what's already in the POM.
- **Persistence for N rows** — `spring-boot-starter-data-jpa` + the existing `ShortLinkRepository`
  (its inherited `saveAndFlush` is what `LinkService.createLink()` already calls) is unchanged and
  sufficient; no batch-insert/bulk-JDBC library is needed because the reused `createLink()` method
  is explicitly one-row-at-a-time by design (`LinkService.java` Javadoc, Candidate A).
- **If risk-register.md's rate-limiting recommendation (R-BULK-1) is accepted at Gate 2/3** — a
  request-level limiter for the new batch endpoint would reuse **Bucket4j 8.19.0**, already a
  dependency (used today by `RateLimiterService.java`). No new rate-limiting library would be
  needed even under that scenario.

## Explicitly checked and ruled out
- No async/queue/job-runner dependency is implied — `prework/prd-v0.md` Section 4.4 already
  confirmed (and this audit independently confirms via the same `pom.xml` read) there is no
  existing async task executor or message-queue dependency in this project, and the proposed
  design deliberately stays synchronous request/response to avoid needing one.
- No new HTTP client dependency — this is a server-side endpoint, not a caller of another service.
- No batch-specific base62/encoding library — `service/CodeGenerator.java` (already present, ~40
  lines, previously audited and deliberately kept dependency-free per its own Javadoc) is reused
  per item unchanged.

## Conclusion
Zero new entries required in `pom.xml`. The feature is buildable entirely on top of dependencies
already declared and in use.
