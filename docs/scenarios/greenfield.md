# Scenario 1: Greenfield — `url-shortener-core`

**Command:** `/run-sdlc java-spring url-shortener-core greenfield --mode=hybrid`
**Full trail:** [`.claude/output/url-shortener-core/`](../../.claude/output/url-shortener-core/)

## Decomposition
Entered at **STEP-0** (only greenfield does). `concept-refinement`
normalized 3 real ambiguities from the raw `idea.md` (expiry rule,
analytics depth, alias-collision behavior) into explicit proposals.
`market-research` compared against Bitly/TinyURL/is.gd and confirmed
`java-spring` had no architectural friction. From there, the standard
pipeline: PRE-WORK → STEP-1 Discovery (5-risk register) → STEP-2 Spec
(4 endpoints, 9 ACs) → STEP-3 Design (DB-unique-constraint concurrency
strategy, chosen specifically to satisfy the collision-safety AC
without app-level locking) → STEP-4 Implementation.

## Orchestration
6 HITL gates crossed (0, 0.5, 1, 2, 3, 6), all logged in
[`_decisions.yaml`](../../.claude/output/url-shortener-core/_decisions.yaml)
with the reasoning behind each. Zero retries, zero rollbacks — a clean
run start to finish.

## Validation
`build-verdict` found no toolchain on `PATH`, located a real
IntelliJ-managed JDK + Maven, and **corrected** the stack manifest
(Java 21→19, Spring Boot 3.3→3.1.4) to match what's actually installed
— documented, not silently patched over. `mvn compile` → BUILD SUCCESS.
14 real JUnit tests including a 20-thread concurrency test for the
collision-safety AC → 14/14 PASS. `grading-feedback` scored **0.95**
→ **COMPLETE**.

## What This Scenario Demonstrates
Requirement normalization under genuine ambiguity, and that "build
green" means a real compiler invocation — including honestly
correcting a stale assumption (Java 21) rather than asserting success.
