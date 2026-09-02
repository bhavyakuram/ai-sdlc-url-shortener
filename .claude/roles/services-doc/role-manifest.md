---
role_id: services-doc
posture: doc
contract_posture: consumer
---

# Role: services-doc

Audit-only / documentation-improvement posture. No code generation.

## layers_in_scope
`api`, `service` (read-only)

## agents_in_scope / agents_skipped
`agents_skipped`: `generator`, `test-generation` (nothing is generated).
`build-verdict` still runs but only to confirm the pre-existing code
still compiles unchanged. STEP-6 audit agents run at full strength —
this role exists specifically to produce audit/documentation output.

## contract_posture
`consumer` — reads the existing contract, never changes it.

## Used By
Test-and-documentation-improvement scope items called out in the
assignment's Section 3 (Scope).
