---
agent: requirement-ingestion
input: inputs/url-shortener-bulk-shorten/supporting-docs/request.md
---

# PRD v0: Bulk Shorten

## Normalized Requirement
Add a way to create multiple short links in a single API call, reusing
the existing single-create logic (`LinkService.createLink`) per item.

## Flagged Ambiguities (explicitly called out in the raw request itself)
1. **Partial-failure behavior is unspecified.** The raw request asks me
   to use judgment. Proposed: **per-item independent outcomes** — one
   bad item (invalid URL, taken alias) does not fail the whole batch;
   the response enumerates a result (success or error) per submitted
   item. Rationale: an all-or-nothing batch would force a caller to
   resubmit N-1 good items just because 1 was bad, which is worse UX
   for zero technical benefit (there is no cross-item invariant to
   protect — unlike, say, a financial transaction).
2. **Batch size limit is unspecified.** Proposed: **max 20 items** per
   call. Rationale: no requirement suggests bulk import at scale (that
   would be a different feature — CSV upload, background job); 20 is
   enough for the stated use case ("shorten a whole list") without
   opening a request-size-driven DoS vector on an unauthenticated
   endpoint (this project has no auth per idea.md's original v1 scope).

Both proposals go to STEP-2 `feature-spec` as *proposed* normalizations,
to be confirmed at Gate 1/Gate 2 — not treated as already decided.
