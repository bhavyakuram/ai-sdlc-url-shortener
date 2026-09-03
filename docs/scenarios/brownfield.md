# Scenario 2: Brownfield — `url-shortener-bulk-shorten` (agentic mode, real trigger)

**Triggered directly**: `/sdlc-launcher java-spring url-shortener-bulk-shorten services-mod --mode=agentic`
**Full trail:** [`.claude/output/url-shortener-bulk-shorten/`](../../.claude/output/url-shortener-bulk-shorten/)

## Genuine brownfield — real code, read fresh every phase
Every subagent this run re-read the actual `service-java-spring/` source
rather than assuming its shape — and it had real shape to discover:
the completed `url-shortener-core` run had already added rate
limiting, geo lookup, and reserved-code checks that a naive read of
the *original* design docs wouldn't predict.

## Two real findings from Gate 1
1. **Rate-limit amplification**: the batch endpoint would have
   silently inherited the redirect-only rate-limit exclusion (FR-9),
   turning it into a 100x amplification vector. Closed with a **new**
   limiter scoped just to the batch endpoint — 20 req/min/IP, sized
   against the endpoint's actual cost (up to 200 sequential DB
   round-trips per request can hold a worker thread), not copied by
   analogy from the redirect limiter's 100/60s.
2. **Spring AOP self-invocation**: if the batch loop lived inside
   `LinkService` calling `this.createLink()`, `@Transactional`'s
   proxy-based enforcement would be silently bypassed per item — a
   real, easy-to-miss Spring gotcha. Closed **structurally**: a new
   `BulkLinkOrchestrator` service class calls `LinkService` through
   its injected proxy; `LinkService.java` itself gets zero edits,
   verified by a SHA-256 hash comparison (captured at read-time,
   matched at completion), not just visual inspection.

## A subagent interrupted, resumed — not restarted
`generator`'s dispatch hit a session rate limit mid-task, right after
finishing the main source files. Rather than starting a fresh subagent
from scratch (losing all its file-reading and design context),
`SendMessage` resumed the *exact same* agent from where it stopped —
it finished the 3 remaining test files and its own verification pass
with full continuity.

## Learning applied across runs, not just within one
The generator independently re-derived the "service-owned carrier
types, never import `api.dto`" pattern the previous (hybrid-mode)
brownfield run learned the hard way — without being told to. And
STEP-3's `api-contract-delta.yaml` was explicitly cross-checked
field-by-field against `feature-spec.md` before being written, citing
the exact mismatch bug from the earlier greenfield run as the reason.

## Validation
`mvn compile` → BUILD SUCCESS, first attempt, zero retries (unlike
the greenfield run's one real BLOCKER). 50/50 tests pass (15 new +
all 35 pre-existing, zero regression). Verified with **2 consecutive**
`mvn clean test` runs — a lesson learned directly from the greenfield
run's real H2-durability-test bug, applied proactively here rather
than trusting one green result. 91.5% coverage. `grading-feedback`
scored **0.985** → **COMPLETE**.

## What This Scenario Demonstrates
Real, current-state codebase reasoning (not a snapshot-in-time
assumption); risk findings that actually change the design rather
than being noted and ignored; and — maybe the most concretely
"agentic" thing in this whole project — an interrupted subagent
picked back up mid-task with full context, rather than being thrown
away and redone.
