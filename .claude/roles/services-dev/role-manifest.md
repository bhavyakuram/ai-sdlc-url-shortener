---
role_id: services-dev
posture: dev
contract_posture: producer
---

# Role: services-dev

New module/endpoint work, backend-only.

## layers_in_scope
`api`, `service`

## agents_in_scope / agents_skipped
Skips `ux-design` (STEP-2) — no UI in scope for a services-only role.
Everything else in scope.

## contract_posture
`producer`.

## Used By
Adding a genuinely new endpoint (e.g. bulk-shorten) to an existing
service where the surrounding architecture is already settled.
