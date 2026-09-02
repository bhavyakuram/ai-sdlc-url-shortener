---
snapshot_sha: efe8899
generated_by: codebase-context (PRE-WORK)
---

# Codebase Context @ efe8899

## In-Scope Files (given role=services-mod, layers=[api, service])
```
api/LinkController.java          <- will gain a new @PostMapping
api/ApiExceptionHandler.java     <- may need a new mapping if bulk introduces a new exception type
api/dto/CreateLinkRequest.java   <- reusable as-is for each batch item
api/dto/LinkResponse.java        <- reusable as-is for each successful item
service/LinkService.java         <- will gain a new method, reusing createLink() per item
```
`data/` is explicitly OUT of scope for `services-mod` here — the
enhancement request doesn't need a schema change (no new entity/table),
confirmed at STEP-1 impact-analysis.

## Drift vs. Standards
None found — the existing code already follows
`stacks/java-spring/standards/` conventions (constructor injection,
no field injection, machine-readable error codes). Nothing to flag.

## Current API Surface (from live grep, not memory)
`POST /links`, `GET /{code}`, `GET /links/{code}/analytics` — 3 mappings in `LinkController.java`.
