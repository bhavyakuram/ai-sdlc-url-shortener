---
snapshot_sha: 0fa28414f8bd
generated_by: codebase-context (PRE-WORK)
---

# Codebase Context @ 0fa28414f8bd

## Analytics-Relevant Code (in scope for this request regardless of eventual posture)
- `service/LinkService.resolveAndRecordClick(code, referrer)` — the
  one write path for `ClickEventEntity`. Called once per redirect.
- `data/ClickEventRepository` — plain `JpaRepository`, no custom
  concurrency handling beyond what JPA/Hibernate does by default.
- `data/ClickEventEntity` — no unique constraint (unlike `ShortLinkEntity`'s
  `short_code`) — click events are pure inserts, no update-in-place, so
  there is no obvious write-write conflict surface the way link
  creation had one.

## What "unreliable under load" Could Concretely Mean Here
1. **Lost writes**: does a burst of concurrent redirects to the same
   code ever fail to record a click? (Testable directly.)
2. **Double-counting**: does anything retry a click write on transient
   failure, in a way that could double-record? (Nothing in the current
   code retries — no such mechanism exists to have this bug.)
3. **Read-after-write consistency**: does `getAnalytics` reliably see
   clicks recorded microseconds earlier under concurrent load?

Only (1) and (3) are actually testable against the real code as it
stands — (2) doesn't apply because no retry logic exists to cause it.
