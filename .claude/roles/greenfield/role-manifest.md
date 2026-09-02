---
role_id: greenfield
posture: greenfield
contract_posture: producer
---

# Role: greenfield

Pure ideation-to-app posture. The only role that triggers STEP-0.

## layers_in_scope
`api`, `service`, `data` (nothing exists yet — this role scaffolds all
of it).

## agents_in_scope / agents_skipped
Triggers STEP-0 (`concept-refinement`, `market-research`,
`ux-prototype`) before PRE-WORK. From PRE-WORK onward, behaves like
`fullstack`.

## contract_posture
`producer`.

## Used By
The very first run against an empty repo — this is what `triage`
recommends when it classifies `feature_shape=greenfield-app`.
