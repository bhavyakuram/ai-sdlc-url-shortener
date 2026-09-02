---
agent: feature-spec
---

# Feature Spec: Bulk Shorten

## FS-5 Create multiple links
`POST /links/bulk` — accepts `{ items: [CreateLinkRequest, ...] }`,
1-20 items (per prd-v0.md's proposed cap).
- Returns `400` (whole request rejected) if `items` is empty or has
  more than 20 entries — this is a request-level check, before any
  item is processed.
- Otherwise returns `200` always, with a per-item result list in the
  **same order** as submitted:
  `{ results: [ {status: "created", link: LinkResponse} | {status: "error", error: ErrorResponse}, ... ] }`
- Each item is processed independently, reusing the exact same
  validation/collision rules as `POST /links` (FS-1) — an item that
  would 400/409 individually gets `status: "error"` in its slot here,
  not a whole-batch failure.
- No cross-item transaction: item 2 failing does not roll back item 1.
  This is a deliberate design choice (see risk R2/prd-v0.md) — there is
  no cross-item invariant in this domain that would justify all-or-nothing.
