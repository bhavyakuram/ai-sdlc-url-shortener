---
role_id: fullstack
posture: dev
contract_posture: producer
---

# Role: fullstack

**Default role.** All layers in scope; producer of the API contract.

## layers_in_scope
`api`, `service`, `data`

## agents_in_scope / agents_skipped
All PRE-WORK, STEP-1..STEP-6 agents in scope. STEP-0 in scope only when
`feature_shape=greenfield-app` (see `roles/greenfield/`).

## contract_posture
`producer` — this role defines the API contract rather than consuming
one defined elsewhere.

## Used By
The greenfield scenario (build the URL shortener from scratch) and any
full end-to-end brownfield enhancement that touches all three layers.
