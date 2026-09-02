---
agent: planner
---

# Sprint Plan — Bulk Shorten

Single wave (5 files — below the 5+ multi-wave threshold, borderline;
kept as one wave since all 5 are tightly coupled around one endpoint):

1. `api/dto/BulkCreateRequest.java` (new)
2. `api/dto/BulkItemResult.java` (new)
3. `api/dto/BulkCreateResponse.java` (new)
4. `service/LinkService.java` (**MODIFY** — add `createBulk`, inject `jakarta.validation.Validator` to reuse `CreateLinkRequest`'s existing annotations for per-item validation rather than duplicating the regex — DRY, per `rules/coding-standards.md` No Dead Code / code-reuse policy)
5. `api/LinkController.java` (**MODIFY** — add `createBulk` mapping)

`ApiExceptionHandler.java` is NOT modified — per-item errors never
reach it; they're caught inside `LinkService.createBulk` itself.
