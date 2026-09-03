# STEP-1 Discovery: Feasibility Report — url-shortener-core

**Phase:** STEP-1 · **Agents:** `feasibility` -> `dependency-audit` -> `impact-analysis`
**Stack:** java-spring · **Role:** greenfield · **Mode:** agentic · **Platform:** none
**Inputs read:** `prework/prd-v0.md` (full, all 12 FRs + 6 NFRs + 3 STEP-3 friction points),
`shared-context/java-spring/snapshots/greenfield-baseline/architecture-context.md`,
`stacks/java-spring/stack-manifest.md`, `stacks/java-spring/stack-skills.yaml`

---

## Part 1 — `feasibility`

### Verdict: **BUILDABLE AS SPECIFIED — no blocker**

### Method
Cross-checked every Must/Should FR in `prework/prd-v0.md` Section 2.3 against (a) the declared
stack capabilities in `stacks/java-spring/stack-manifest.md` (`api:rest`, `services:jvm`,
`data:relational`, `build:maven`, `test:junit`) and (b) the 3 carried-forward friction points in
PRD Section 2.5. Confirmed the repo is genuinely empty so nothing here is fighting existing code:

```
$ find . -iname "*.java" -not -path "*/node_modules/*"
(no output)
$ find . -iname "pom.xml"
(no output)
```

### FR-by-FR feasibility

| FR | Feasible on this stack? | Notes |
|---|---|---|
| FR-1 create endpoint | Yes | Standard `@RestController` + `@PostMapping`, `api:rest` capability covers it. |
| FR-2 redirect 302 | Yes | `RedirectView` / `ResponseEntity.status(302).location(...)`, no library needed. |
| FR-3 scheme/length validation | Yes | Bean Validation (`spring-boot-starter-validation`) + a custom `@Pattern`/validator for scheme allowlist. Not yet in `stack-manifest.md`'s dependency list — flagged in Part 2 below, not a blocker (it's a standard Spring Boot starter, not a new capability token). |
| FR-4 collision-safe codes | Yes | `data:relational` capability + `@Column(unique = true)` + catch `DataIntegrityViolationException` on insert-retry — the exact pattern `market-research.md` Section 3.1 confirmed as idiomatic JPA. No new dependency. |
| FR-5 durable persistence | Yes, **conditionally** | Requires H2 running in **file mode**, not the Spring Boot default in-memory mode. This is a `spring.datasource.url` configuration decision (`jdbc:h2:file:...` vs `jdbc:h2:mem:...`), not a code or dependency blocker — but it must be made explicitly at STEP-3/STEP-4, since Spring Boot's H2 auto-configuration defaults to in-memory and would silently violate FR-5/NFR "Durability" if left on the default. Flagged as a **risk**, not a feasibility blocker (see risk-register.md R-1). |
| FR-6 no auth/session | Yes | Absence of a concern is trivially feasible — no Spring Security dependency needed, confirmed nothing in the FR set requires it. |
| FR-7 30-day expiry, 404 on expired | Yes | A `createdAt`/`expiresAt` timestamp column + a check in the redirect service method before issuing 302. No scheduler/cron dependency required — expiry is evaluated lazily on read, matching FR-2/FR-7's stated behavior (404 the moment a request lands on an expired code, not proactive deletion). |
| FR-8 custom code | Yes | Same unique-constraint mechanism as FR-4; validation of the 3-32 char base62 shape is a regex, no new dependency. |
| FR-9 rate limiting 100 req/min | Yes, **needs a library decision** | Spring Boot ships no first-party rate limiter (PRD friction point #2). Feasible either as a hand-rolled in-process token bucket or via Bucket4j — see Part 2 for the concrete recommendation. Not a blocker either way. |
| FR-10 click events + stats | Yes, **needs a new dependency** | Country-level geo lookup requires an offline IP database (PRD friction point #3) — not in `stack-manifest.md`'s declared capability vocabulary today. See Part 2 for the concrete recommendation and the licensing caveat that makes this the one genuinely non-trivial dependency decision in this feature. |

### NFR feasibility spot-check
- **Redirect latency p95 < 50ms:** feasible on Tomcat's conventional thread-pool model (PRD friction point #1 — no virtual threads on Java 19/Spring Boot 3.1.4-3.2.x) at the stated scale (a 100 req/min-per-code rate ceiling is far below thread-pool exhaustion). No architectural change needed to hit this target; flagged only so STEP-4 doesn't reach for `--enable-preview` virtual threads, which `market-research.md` Section 3.2 already ruled out as unavailable/unstable on this JDK.
- **Collision rate: 0 duplicate active codes under concurrency:** feasible via DB unique constraint (not application-level check-then-insert), which is a correctness guarantee from the database, not from Java-level locking — this is the right mechanism for concurrency safety and is explicitly required by FR-4, not left to STEP-4's discretion.
- **Abuse resilience (no 5xx at 10x rate-limit threshold):** feasible provided the rate limiter itself fails safe (returns 429, never throws unhandled) — a specific implementation requirement to carry into STEP-3 design, not a feasibility blocker.

### Blockers found: **none**
### Conditions carried forward to STEP-2/STEP-3 (not blockers, but must not be silently decided by `generator`)
1. H2 must be explicitly configured in file mode (FR-5) — default Spring Boot H2 auto-config is in-memory.
2. Rate-limiter implementation choice (hand-rolled vs. Bucket4j) — see Part 2.
3. Geo-IP dependency choice and its licensing/operational model — see Part 2.

---

## Part 2 — `dependency-audit`

### Method
Read `prework/prd-v0.md` in full and `stacks/java-spring/stack-manifest.md`'s declared capability
vocabulary (`api:rest`, `services:jvm`, `data:relational`, `build:maven`, `test:junit`). Because
the repo has no `pom.xml` yet (confirmed above — greenfield, nothing to diff against), every
dependency below is a **new addition to the first-ever `pom.xml`**, not a delta against an
existing dependency tree. Real `WebSearch` calls (quoted verbatim below) were used to confirm
current version/maintenance/CVE status for every dependency that is not a Spring Boot starter,
per `rules/architecture.md` Proof Over Promise — no artifact coordinate or CVE claim below is
asserted from memory.

### 2.1 Dependencies required by the PRD as specified

| # | Dependency | Maven coordinates | Required by | New capability token needed? |
|---|---|---|---|---|
| 1 | Spring Web | `org.springframework.boot:spring-boot-starter-web` | FR-1, FR-2 (REST + redirect) | Already covered by `api:rest` |
| 2 | Spring Data JPA | `org.springframework.boot:spring-boot-starter-data-jpa` | FR-4, FR-5, FR-8, FR-10 (persistence, unique constraint) | Already covered by `data:relational` |
| 3 | H2 Database | `com.h2database:h2` (runtime scope) | FR-5 (durable, file-mode persistence) | Already covered by `data:relational` — but **must be run in file mode**, see feasibility Part 1 condition #1 |
| 4 | Bean Validation | `org.springframework.boot:spring-boot-starter-validation` | FR-3 (scheme/length), FR-8 (custom code shape) | Not yet listed in `stack-manifest.md`'s dependency examples, but it is a standard Spring Boot starter under the already-declared `api:rest`/`services:jvm` capabilities — no new capability token needed, just needs to be added to the manifest's dependency list at STEP-3. |
| 5 | Spring Boot Test + JUnit 5 + Mockito | `org.springframework.boot:spring-boot-starter-test` | `test:junit` (all FRs, via `rules/testing.md`) | Already covered by `test:junit` |

None of #1-5 carries a CVE concern worth flagging — these are the current-generation Spring Boot
starter BOM artifacts, patched via routine `spring-boot-starter-parent` version bumps, which is
already `dependency-audit`'s (STEP-1/STEP-6.2) ongoing job per `rules/security.md` Dependency
Hygiene, not a one-time greenfield decision.

**H2 CVE note (relevant because FR-5 makes H2 load-bearing, not incidental):**
```
WebSearch("H2 database CVE 2022-45868 file mode security")
WebSearch("H2 database console CVE-2021-42392 remote code execution unauthenticated JNDI")
```
Two historical H2 CVEs are worth naming explicitly since this feature leans on H2 for real
persistence (not just tests):
- **CVE-2021-42392** — H2 Console JNDI lookup allows unauthenticated RCE (JNDI/LDAP injection,
  Log4Shell-shaped), fixed in H2 2.0.206 by restricting JNDI URLs to the local `java` protocol.
- **CVE-2022-45868** — H2's `-webAdminPassword` CLI argument exposes the admin console password
  in cleartext to any local process listing, fixed in 2.2.220.
- **Mitigation, both:** (a) pin `com.h2database:h2` to >= 2.2.220 (well past both fix versions —
  current mainline is 2.3.x), and (b) **the H2 web console (`spring.h2.console.enabled`) must stay
  `false`/unset in any profile that isn't purely local-dev**, since FR-6 already establishes this
  service has no auth layer — an exposed H2 console on a public deployment would be a direct
  unauthenticated-RCE path layered on top of an already-authless service. This is carried into
  `risk-register.md` (R-6).

### 2.2 Dependencies required by STEP-3 friction points (PRD Section 2.5), evaluated for a concrete pick

#### Friction point #2 — Rate limiter (FR-9)
**Recommendation: Bucket4j**, coordinates confirmed live via search rather than assumed:
```
WebSearch("Bucket4j Java rate limiting library maven maintained latest version CVE 2026")
```
Result: latest release is **8.19.0 (May 2026)**, actively maintained (last release the same
month as this search), Maven coordinates:
```xml
<dependency>
  <groupId>com.bucket4j</groupId>
  <artifactId>bucket4j_jdk17-core</artifactId>
  <version>8.19.0</version>
</dependency>
```
No CVE surfaced for Bucket4j in the search results. Assessment: this is a small, dependency-light,
single-purpose token-bucket library (in-process, no Redis/cluster backend needed for this MVP's
in-process rate-limit requirement per FR-9) — reasonable, low-risk pick. **Alternative considered
and rejected:** a hand-rolled `ConcurrentHashMap<key, TokenBucket>` (a few dozen lines, zero new
dependency) was the other option `market-research.md` flagged. Recommending Bucket4j over
hand-rolling because FR-9's exact semantics (per-(IP, code) key, 100 req/min, 429 beyond
threshold) map directly onto Bucket4j's documented API with less surface for an off-by-one
concurrency bug than a hand-rolled bucket would introduce — this trade-off should be confirmed,
not silently assumed, at STEP-3 (this is a recommendation, not a STEP-3 design decision).

#### Friction point #3 — Geo-IP lookup (FR-10)
**Recommendation: MaxMind GeoLite2-Country (offline `.mmdb` file) via the `com.maxmind.geoip2:geoip2` reader library**, confirmed live:
```
WebSearch("com.maxmind.geoip2 java library maven CVE GeoLite2 offline country lookup license")
WebSearch("MaxMind GeoLite2 free license key account required download 2026 EULA")
```
```xml
<dependency>
  <groupId>com.maxmind.geoip2</groupId>
  <artifactId>geoip2</artifactId>
  <version>5.2.0</version>
</dependency>
```
This is the standard library for exactly this use case (country-level, offline, no live network
call per PRD FR-10 and `rules/mcp-convention.md`'s enabled-integration set, which does not include
a live geo API). No CVE surfaced against the library itself in the search results (the results
noted only transitive-dependency-level CVEs, which is normal dependency-hygiene surface to
monitor at STEP-6.2, not a reason to reject the pick).

**This is the one dependency decision that is NOT a pure "add a Maven artifact and go" choice —
named explicitly here rather than left implicit, per PRD friction point #3's own instruction:**
Since late 2019, MaxMind has required a **free account + accepted EULA + a license key** to
download the underlying `GeoLite2-Country.mmdb` database file — the file itself is **not**
distributed as a Maven artifact and is **not** bundled by the `geoip2` reader library. As of this
search, license keys also **expire every 90 days** without reconfirmation. This means:
- The `.mmdb` file must be fetched out-of-band (a build step or an ops runbook), not resolved by
  Maven — this is an operational dependency, not just a code dependency, and belongs on STEP-3's
  design surface explicitly.
- A recurring credential-rotation task (license key renewal) is now a standing operational
  requirement for this feature, which did not exist before FR-10.
- **This is carried into `risk-register.md` as R-7**, since it's a real, non-trivial operational
  risk (a stale/expired key silently breaks FR-10 without breaking the build or any other FR),
  not merely a licensing footnote.

**Also recommending:** `stacks/java-spring/stack-manifest.md`'s capability vocabulary (currently
`api:rest`, `services:jvm`, `data:relational`, `build:maven`, `test:junit`) has no geo-IP token.
Per `market-research.md` Section 3.2 point 3 and PRD friction point #3, this should be named
explicitly (e.g. a `data:geoip-offline` token) and added to the manifest at STEP-3 rather than
`generator` introducing the dependency ad hoc mid-implementation — this is a STEP-3 design-gate
action item, not something this audit can resolve unilaterally.

### 2.3 Explicitly evaluated and NOT recommended as a dependency
- **Base62 encoding library:** not needed. Base62 (alphabet `[0-9A-Za-z]`) encode/decode for a
  7-32 char code is ~15 lines of hand-written Java (divide-and-remainder against a fixed
  alphabet) — pulling in a dependency (e.g. `commons-codec`, which only ships Base32/Base64
  anyway, not Base62) would be unjustified surface area for something `market-research.md`
  Section 1.4 already confirmed is a standard, trivial technique on this exact stack.
- **A live/networked geo-IP API** (e.g. ip-api.com, ipinfo.io): explicitly rejected — FR-10 and
  `rules/mcp-convention.md`'s enabled-integration set (code-graph only) both rule out a live
  external call on the redirect hot path; an offline `.mmdb` lookup keeps geo resolution off the
  network entirely, which also directly serves the NFR-latency target (p95 < 50ms).
- **A distributed cache/sequence service** (e.g. Redis-backed ID generation, as seen in the
  `William-Nogueira/spring-url-shortener` comparable from `market-research.md` Section 1.4): not
  needed at this MVP's scale — the DB-unique-constraint-plus-retry approach (FR-4) already
  satisfies the collision NFR without a new infrastructure dependency; adding Redis here would be
  premature complexity for a stated single-instance MVP.

### 2.4 Dependency-audit summary table

| Dependency | New? | CVE named? | Blocker? |
|---|---|---|---|
| spring-boot-starter-web | Yes (first pom.xml) | No | No |
| spring-boot-starter-data-jpa | Yes (first pom.xml) | No | No |
| com.h2database:h2 (>= 2.2.220) | Yes (first pom.xml) | Yes — CVE-2021-42392, CVE-2022-45868 (both fixed pre-2.2.220, mitigation = version pin + console disabled) | No, if pinned + console off |
| spring-boot-starter-validation | Yes (first pom.xml) | No | No |
| spring-boot-starter-test | Yes (first pom.xml) | No | No |
| com.bucket4j:bucket4j_jdk17-core:8.19.0 | Yes (new, STEP-3 to confirm) | None found | No |
| com.maxmind.geoip2:geoip2:5.2.0 + GeoLite2-Country.mmdb | Yes (new, STEP-3 to confirm) | None found on the library; **operational risk on the license-key/EULA process, not the code** | No (but see R-7) |

---

## Part 3 — `impact-analysis`

### Verdict: **Explicitly N/A — greenfield, confirmed, not silently skipped**

Per this agent's input contract (`codebase-context.md`, `feasibility-report.md`) and its stated
purpose ("Determines what existing code, contracts, and data will be affected —
**brownfield-critical**"), this analysis is structurally inapplicable to a greenfield run, and
that inapplicability is verified directly rather than assumed:

```
$ find . -iname "*.java" -not -path "*/node_modules/*"
(no output)
$ find . -iname "pom.xml"
(no output)
$ find .claude/shared-context/java-spring -type f
.claude/shared-context/java-spring/snapshots/greenfield-baseline/architecture-context.md
.claude/shared-context/java-spring/snapshots/greenfield-baseline/manifest.json
```

- **`codebase-context.md` does not exist** for this stack (only `architecture-context.md` does),
  consistent with `posture-feasibility`'s PRE-WORK finding (`prd-v0.md` Part 3) that this is a
  true cold bootstrap — there is no indexed existing codebase for this agent to diff against.
- **No `.java` sources and no `pom.xml`** — there is no existing code, contract, or data schema
  that this feature could impact, because nothing besides this run's own upcoming output exists
  yet.
- **`code-graph tier3` (find_callers/find_callees)** — the capability this agent's contract
  declares as required — has nothing to query against: `find_callers`/`find_callees` operate over
  an indexed call graph, and there is no code to index. The capability requirement is trivially
  and correctly satisfied by "zero callers, zero callees, zero affected contracts" rather than by
  skipping the check.

**Conclusion:** Impact analysis is **N/A by design** for this run, per the same reasoning
`roles/greenfield/role-manifest.md` already establishes for the `greenfield` role ("nothing exists
yet — this role scaffolds all of it"). This is recorded explicitly here so `risk-analysis` (Part
4 / `risk-register.md`) does not have to re-derive it, and so Gate 2 sees an affirmative N/A
rather than an absent section.

---

## Summary for Gate 2

| Agent | Verdict |
|---|---|
| `feasibility` | **BUILDABLE, no blocker.** 3 conditions carried to STEP-3 (H2 file-mode config, rate-limiter pick, geo-IP dependency + capability-token addition). |
| `dependency-audit` | 5 standard Spring Boot Framework dependencies (no CVE concern) + 2 deliberate new picks: **Bucket4j 8.19.0** (rate limiting, no CVE found, actively maintained) and **MaxMind geoip2 5.2.0 + GeoLite2-Country.mmdb** (geo lookup, no library CVE, but a real licensing/credential-rotation operational risk — not a code risk). H2 pinned >= 2.2.220 with console disabled to close 2 historical CVEs. |
| `impact-analysis` | **Explicitly N/A** — verified via direct repo inspection (zero `.java` files, zero `pom.xml`, no `codebase-context.md`), consistent with PRE-WORK's `posture-feasibility` MATCH finding. |

No BLOCKER findings from STEP-1's first three agents. Proceeding to `risk-analysis`
(`risk-register.md`).
