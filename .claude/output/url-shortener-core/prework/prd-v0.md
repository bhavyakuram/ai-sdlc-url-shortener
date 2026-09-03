# PRD v0: url-shortener-core

**Phase:** PRE-WORK · **Agents:** `triage` -> `requirement-ingestion` -> `posture-feasibility`
**Stack:** java-spring · **Filed role:** greenfield · **Mode:** agentic · **Platform:** none

---

## Part 1 — `triage` (agent: `agents/prework/triage.md`, skill: `skills/prework/triage/SKILL.md`)

### Output Contract: `prework/triage-verdict.md` fields

| Field | Value |
|---|---|
| `feature_shape` | **`greenfield-app`** |
| `recommended_role` | **`greenfield`** (matches filed role — no NARROW/EXPAND needed) |
| `retry_budget` | **5** (per role lane) |

**Evidence for `feature_shape=greenfield-app`:**
- `.claude/inputs/url-shortener-core/ideation/idea.md` is present (the greenfield-only input path per `rules/input-path.md`) and no `jira/` export exists for this feature:
  ```
  $ find .claude/inputs/url-shortener-core -type f
  .claude/inputs/url-shortener-core/ideation/idea.md
  ```
- The idea.md text itself is phrased as new-product ideation from zero ("We need a URL shortener service, built from scratch"), not a change request against existing behavior.
- No prior code surface exists to be enhancing/fixing/refactoring — verified directly:
  ```
  $ find . -iname "*.java" -not -path "*/node_modules/*"
  (no output)
  $ find . -iname "pom.xml" -o -iname "build.gradle"
  (no output)
  ```
- The shared-context bootstrap already reached the same classification independently, recorded before this PRE-WORK run:
  ```
  $ cat .claude/shared-context/java-spring/snapshots/greenfield-baseline/manifest.json
  {"snapshot_sha": "greenfield-baseline", "stack": "java-spring", "cache_status": "cold-bootstrap",
   "reason": "No existing service-java-spring/ code — cleared for this fresh trigger."}
  $ sed -n '1,6p' .claude/shared-context/java-spring/snapshots/greenfield-baseline/architecture-context.md
  ---
  snapshot_sha: greenfield-baseline
  stack: java-spring
  ---
  No existing service code. `feature_shape=greenfield-app`. Layers per
  `stacks/java-spring/stack-manifest.md`: api, service, data (no
  frontend layer — see `agents/step0/ux-prototype.md`'s conditional
  firing rule).
  ```

**Recommended role reasoning:** `roles/greenfield/role-manifest.md` states greenfield is "what `triage` recommends when it classifies `feature_shape=greenfield-app`," and that its `layers_in_scope` (`api`, `service`, `data`) is exactly what `concept.md` Section 4 scoped the MVP to (no frontend feature is proposed anywhere in the MoSCoW list). The filed role (`greenfield`) and the recommended role are the same — **no role-feasibility mismatch to raise at Gate 1**.

**Retry budget reasoning:** `rules/retry-policy.md` sets a hard default of 5 retries per role lane, with `adaptive-gate` permitted to calibrate 3-8 *based on historical pass rate for the stack+role combination*. Checked the actual history source:
```
$ cat .claude/run-history/_online-learning.yaml
matrix_rows: {}
```
`matrix_rows` is empty — there is no `java-spring` + `greenfield` historical pass-rate row to calibrate from yet (this is the first run ever recorded). Per `adaptive-gate`'s own contract, with zero history it has nothing to calibrate against, so triage sets `retry_budget` to the **uncalibrated default of 5**, not a tightened or loosened value. This matches what `role-resolver` already wrote to `_role-context.yaml` (`retry_budget: 5`, with the identical "no history to calibrate from" note) — triage's independent read is consistent with, not overridden by, that prior resolution. **Flag for `pattern-extractor`:** once this run completes (PASS or SAFE-STOPPED), it should be the first row written into `matrix_rows` for `java-spring`/`greenfield`, so the *next* greenfield run on this stack has real data to calibrate against.

---

## Part 2 — `requirement-ingestion` (agent: `agents/prework/requirement-ingestion.md`, skill: `skills/prework/requirement-ingestion/SKILL.md`)

**Input read in full:** `.claude/inputs/url-shortener-core/ideation/idea.md` (22 lines, no `jira/` or `supporting-docs/` entries present for this feature — confirmed by the `find` above). Per `rules/input-path.md`, this ingestion is normalizing that raw input plus the already-Gate-0-approved `step0/concept.md` and `step0/market-research.md`, and folding forward concept.md's ten resolved ambiguities (A1-A10) **as settled inputs**, not re-opening them — concept.md's decisions were already `APPROVED` at Gate 0 per `_decisions.yaml`:
```
gate: 0
decision: APPROVED
notes: "All 10 ambiguity resolutions (A1-A10) and market positioning accepted
        as proposed by the real subagent dispatch."
```

### 2.1 Problem Statement
Build, from scratch, an anonymous URL-shortening backend service (java-spring stack, `api`/`service`/`data` layers only, no accounts, no frontend) that: accepts a long URL and returns a short link; redirects short-link visits to the original URL; records enough data to report clicks/timing/coarse origin; and stays correct and available under concurrent and adversarial load.

### 2.2 Actors (carried forward from concept.md Section 2, unchanged)
| Actor | Role |
|---|---|
| Anonymous Link Creator | `POST`s a long URL, optionally a custom code; gets a short URL back. No login. |
| Link Clicker | Visits `GET /{code}`; triggers redirect + click recording. |
| Service Operator | Deploys/monitors the service; consumes the stats endpoint; no admin-auth layer in v1. |

### 2.3 Normalized Functional Requirements
Each requirement below carries its concept.md MoSCoW id and the ambiguity resolution(s) (A1-A10) it depends on, so this PRD is traceable back to the Gate-0-approved decisions without restating their justification.

**Must Have**
- **FR-1** (M1): `POST` endpoint accepts a long URL and returns a generated short code + full short URL.
- **FR-2** (M2, depends on A9): `GET /{code}` resolves a valid, non-expired code to its long URL via **302 Found**; unknown/expired codes do not redirect.
- **FR-3** (M3, depends on A10): Submitted URL must be `http://`/`https://` scheme only, <= 2048 chars; malformed or disallowed-scheme (`javascript:`, `data:`, `file:`) input is rejected with 400, never silently altered.
- **FR-4** (M4, depends on A6/A7): Codes are 7-char base62 when generated; collisions are prevented via a DB-level unique constraint with retry-on-conflict (generated) or 409 (custom) — no check-then-insert race.
- **FR-5** (M5, depends on A4): Persistence is durable across process restart (H2 file-mode, not pure in-memory).
- **FR-6** (M6): No login/account/session concept anywhere in the request path — this is a hard constraint carried into every downstream phase, not a v1-only shortcut (concept.md Section 6).
- **FR-7** (M7, depends on A1): Every link expires 30 days after creation (fixed default, no per-link override in v1); redirect on an expired code returns 404, not a stale 302.

**Should Have**
- **FR-8** (S1, depends on A5/A6): Caller may supply a custom short code at creation time; first-come-first-served, 3-32 chars, same base62 alphabet, rejected (not mutated) outside that shape.
- **FR-9** (S2, depends on A8): Redirect endpoint enforces a per-(source-IP, short-code) rate limit of 100 req/min, in-process token bucket, `429` beyond threshold.
- **FR-10** (S3, depends on A2/A3): Every successful redirect appends one `ClickEvent` (timestamp, coarse country from offline IP lookup, referrer if present); `GET /links/{code}/stats` returns a summary (total clicks, clicks-by-day, clicks-by-country) — no raw log export/UI.

**Could Have**
- **FR-11** (C1): Clicks-by-day trend already covered by FR-10's summary shape (additive read-side only, no schema change).
- **FR-12** (C2): Referrer capture already folded into FR-10 (zero new input surface — existing HTTP header).

**Explicitly Out of Scope for v1 (Won't Have — concept.md Section 4 W1-W5)**
- No accounts/ownership/edit/delete rights over a link.
- No per-link configurable expiry override.
- No analytics dashboard/export UI (no frontend layer in scope).
- No city/precise geolocation — country-level only.
- No custom domains/branded hosts.

### 2.4 Non-Functional Requirements (from concept.md Section 5, success-metrics baseline)
| NFR | Target |
|---|---|
| Redirect correctness | 100% of non-expired, valid codes resolve correctly |
| Redirect latency | p95 < 50ms |
| Collision rate | 0 duplicate active codes ever persisted, under concurrency |
| Durability | 0 link records lost across a normal process restart |
| Abuse resilience | No 5xx on the redirect endpoint under a single-link flood at 10x the FR-9 rate-limit threshold |
| Analytics completeness | Exactly one `ClickEvent` per successful redirect |

### 2.5 Known Technical Friction Flagged for STEP-3 (from market-research.md Section 3.2, carried forward, not resolved here)
1. Java 19 has no stable virtual threads under Spring Boot 3.1.4 — redirect-path concurrency and FR-9's rate limiter must target the conventional Tomcat thread-pool model, not Loom.
2. Spring Boot ships no first-party rate limiter — FR-9 needs either a small hand-rolled token bucket or a library (e.g. Bucket4j) chosen explicitly at STEP-3.
3. FR-10's country-level geo lookup requires a bundled offline IP database dependency not yet in `stacks/java-spring/stack-manifest.md`'s declared capability vocabulary — needs naming (and possibly adding to the manifest) at STEP-3, not silent introduction by `generator`.

### 2.6 Traceability
This PRD introduces no new ambiguities and re-litigates none of A1-A10 — every FR above cites the concept.md decision it implements. Any *new* ambiguity discovered from here forward belongs to STEP-1 Discovery, not PRE-WORK.

---

## Part 3 — `posture-feasibility` (agent: `agents/prework/posture-feasibility.md`, skill: `skills/prework/posture-feasibility/SKILL.md`)

**Verdict: MATCH**

**Check performed:** does the codebase evidence support the filed posture (`greenfield`), per `rules/posture-feasibility.md`'s purpose — catching cases like an operator filing `doc` while the requirement text implies new code, or (the case that matters here) filing `greenfield` when a real code surface already exists that this should instead be a brownfield/`*-mod` run against.

**Evidence:**
1. **No pre-existing service code anywhere in the repo for this stack:**
   ```
   $ find . -iname "*.java" -not -path "*/node_modules/*"
   (no output)
   $ find . -iname "pom.xml" -o -iname "build.gradle"
   (no output)
   ```
   A `greenfield` posture asserts "nothing exists yet — this role scaffolds all of it" (`roles/greenfield/role-manifest.md`). Zero Java sources and zero build files confirms there is no existing implementation this run could be mistaken for modifying.
2. **`codebase-context.md` does not exist** (only `architecture-context.md` does) in `.claude/shared-context/java-spring/snapshots/greenfield-baseline/`:
   ```
   $ find .claude/shared-context/java-spring -type f
   .claude/shared-context/java-spring/snapshots/greenfield-baseline/architecture-context.md
   .claude/shared-context/java-spring/snapshots/greenfield-baseline/manifest.json
   ```
   Per `rules/shared-context.md`, `codebase-context.md` is the artifact that indexes *existing* code; its absence is itself evidence there is nothing to index — consistent with a true cold bootstrap, not just an unindexed brownfield repo. The bootstrap snapshot's own `manifest.json` states the reason explicitly: `"No existing service-java-spring/ code — cleared for this fresh trigger."`
3. **The requirement language matches a build-from-scratch ask, not a modification ask:** idea.md line 3 reads "We need a URL shortener service, built from scratch" — there is no language implying an existing endpoint, table, or behavior is being changed (the inverse of the `posture-feasibility.md` example scenario, where `doc` posture is filed but the text implies new code — here `greenfield` posture is filed and the text explicitly confirms new code from zero).
4. **`_role-context.yaml`'s already-resolved role (`greenfield`) and `layers_in_scope: [api, service, data]`** align with `roles/greenfield/role-manifest.md`'s definition, and `triage`'s independent classification above (`feature_shape=greenfield-app`) agrees with the filed role rather than contradicting it.

**Conclusion:** No posture/evidence mismatch. Per `rules/posture-feasibility.md`, since this check found MATCH (not a mismatch), there is nothing to surface as a flagged warning at Gate 1 — the run proceeds without RATIFY/EXPAND/NARROW/NO-GO being invoked. Nothing here or in Part 1 changes the role, stack, or mode already frozen for this run.

---

## Summary for the launcher / Gate 1

| Agent | Verdict |
|---|---|
| `triage` | `feature_shape=greenfield-app`, `recommended_role=greenfield` (matches filed role), `retry_budget=5` (uncalibrated default — `run-history/_online-learning.yaml` has zero rows for `java-spring`/`greenfield`) |
| `requirement-ingestion` | `prework/prd-v0.md` (this file, Part 2) — 12 FRs (7 Must, 3 Should, 2 Could) traced to concept.md A1-A10; 6 NFRs; 3 STEP-3 friction points carried forward, unresolved by design |
| `posture-feasibility` | **MATCH** — filed `greenfield` posture is fully supported by the evidence (no existing code, no `codebase-context.md`, requirement text is explicitly build-from-scratch) |

No BLOCKER or mismatch findings from PRE-WORK. Ready to advance to STEP-1 Discovery pending the operator's Gate 1 decision.
