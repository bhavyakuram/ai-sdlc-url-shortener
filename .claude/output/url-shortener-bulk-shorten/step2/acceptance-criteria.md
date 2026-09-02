---
agent: acceptance-criteria
---

# Acceptance Criteria (additive to url-shortener-core's AC-1..AC-9)

## AC-10 (FS-5) All-valid batch
GIVEN 3 valid items with no aliases,
WHEN `POST /links/bulk` is called,
THEN a `200` is returned with 3 `results` entries, all `status: "created"`, each with a distinct generated code.

## AC-11 (FS-5) Mixed batch (partial failure)
GIVEN a batch of 3 items where item 2 has an already-taken alias,
WHEN `POST /links/bulk` is called,
THEN a `200` is returned with 3 results in order: `created`, `error (ALIAS_TAKEN)`, `created` — items 1 and 3 are NOT affected by item 2's failure.

## AC-12 (FS-5) Empty batch
GIVEN `items: []`,
WHEN `POST /links/bulk` is called,
THEN a `400` is returned and no links are created.

## AC-13 (FS-5) Over-limit batch
GIVEN 21 items,
WHEN `POST /links/bulk` is called,
THEN a `400` is returned and NO links are created (request-level rejection, not partial processing of the first 20).

## AC-14 (FS-5, cross-cutting with AC-9) Bulk collision-safety
GIVEN a batch where two items happen to request the same custom alias,
WHEN `POST /links/bulk` is called,
THEN exactly one succeeds (`created`) and the other fails
(`error, ALIAS_TAKEN`) — the existing DB-unique-constraint mechanism
(AC-9's basis) must hold even when both attempts originate from the
same request.
