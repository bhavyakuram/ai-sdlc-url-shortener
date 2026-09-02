---
agent: technical-design
---

# Technical Design: R1 Fix

## Approach
Wrap only the click-write line in `LinkService.resolveAndRecordClick`
in a `try/catch (RuntimeException e)`, log it, and continue to return
`link.getTargetUrl()` regardless. Do **not** wrap the whole method —
`LinkNotFoundException`/`LinkExpiredException` (AC-6/AC-7) must still
propagate normally; only the click-write step gets isolated.

## Why Not `@Async` the Click Write Instead
Considered making the click write fire-and-forget async (would also
solve R1). **Rejected** for this scoped fix: it changes the
transactional/ordering guarantee (`@Transactional` currently covers
the whole method) and is a bigger behavioral change than the audit's
finding justifies — EXPAND_LANES was granted for *this specific*
finding, not an open invitation to redesign the write path. The
try/catch is the smallest change that satisfies AC-15/AC-16.

## Logging (rules/coding-standards.md)
`catch` block logs at WARN with the short code and the exception —
enough to diagnose without re-running the request, per the rule's own
wording — then falls through to the existing `return` statement.
