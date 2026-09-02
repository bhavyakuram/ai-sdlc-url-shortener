---
agent: technical-design
---

# Technical Design: Bulk Shorten

## Component Changes
- `api/dto/BulkCreateRequest.java` — `List<CreateLinkRequest> items`,
  `@NotEmpty @Size(max = 20)` — **request-level** validation only
  (AC-12 empty / AC-13 over-limit). This is the one part of the batch
  that IS all-or-nothing, deliberately.
- `api/dto/BulkItemResult.java` — one slot: `status` ("created" |
  "error"), `link` (nullable `LinkResponse`), `error` (nullable
  `ErrorResponse`).
- `api/dto/BulkCreateResponse.java` — `List<BulkItemResult> results`.
- `LinkController.createBulk(...)` — new `@PostMapping("/links/bulk")`.
- `LinkService.createBulk(List<CreateLinkRequest>)` — new method.

## Key Design Decision: No Cascading Bean Validation on Items
**Deliberate divergence from the idiomatic `@Valid` cascade.** Spring's
normal pattern for validating a list of nested objects
(`@Valid List<CreateLinkRequest>`) rejects the *entire* request with a
400 if *any* item fails validation — that's whole-batch failure,
which FS-5/AC-11 explicitly rules out (an invalid item should get
`status: "error"` in its own slot, not sink the batch).

So per-item validation (targetUrl shape, alias shape) is done **inside**
`LinkService.createBulk`, item by item, catching the same exceptions
`createLink` already throws (`AliasTakenException`) plus a manual
targetUrl-shape check reusing the same regex `CreateLinkRequest` uses —
and building a `BulkItemResult` per outcome instead of letting an
exception propagate to `ApiExceptionHandler`. Only the list-level
`@NotEmpty`/`@Size` check remains a normal Spring validation
(intentionally whole-request, per AC-12/AC-13).

## Concurrency / Collision-Safety (AC-14)
Each item calls the *exact same* `createLink` path as the single-item
endpoint — same DB-unique-constraint mechanism, same
`DataIntegrityViolationException` catch. Two same-alias items within
one batch are processed sequentially (not concurrently) inside a single
request thread, so the second one to hit the DB gets the constraint
violation — deterministic, not a race, and reuses code rather than
inventing a second collision-safety mechanism (`rules/coding-standards.md`
No Dead Code / code-reuse policy — `services-mod` has
`code_reuse.enabled=true`).

## No State Migration
No schema change. `data/` layer untouched, confirmed by
`role-feasibility-pass1` in PRE-WORK.
