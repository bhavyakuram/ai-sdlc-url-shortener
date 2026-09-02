---
snapshot_sha: efe8899
stack: java-spring
generated_by: architecture-analysis (PRE-WORK)
cache_status: MISS vs. greenfield-baseline — code has changed since that snapshot (real service now exists)
---

# Architecture Context: java-spring @ efe8899

**Simplification note** (documented per `rules/architecture.md` Proof
Over Promise): `rules/shared-context.md` specifies the snapshot key as
a content hash of tracked file hashes; this run uses the current git
HEAD short SHA (`efe8899`) as a practical stand-in for that hash — same
invalidate-on-change property for this project's purposes, but not a
byte-for-byte implementation of the spec.

## Layers (confirmed against real code, not just the manifest)
- `api`: `LinkController` (3 mappings), `ApiExceptionHandler`, 5 DTOs
- `service`: `LinkService`, `CodeGenerator`, 3 exception types
- `data`: 2 entities, 2 Spring Data repositories

## Dependency Direction (verified)
`api` → `service` → `data` holds: `LinkController` depends on
`LinkService` only; `LinkService` depends on the two repositories;
neither repository nor entity imports anything from `api`/`service`.
No violation found.

## Existing Patterns to Imitate (rules/standards-path.md: reference
code is "what good looks like")
- Collision-safety via DB unique constraint + catching
  `DataIntegrityViolationException`, not app-level locking
  (`LinkService.createLink`).
- Machine-readable error codes via `ErrorResponse(code, message)`,
  mapped centrally in `ApiExceptionHandler`.
- Constructor injection throughout (no field injection, no Lombok).
