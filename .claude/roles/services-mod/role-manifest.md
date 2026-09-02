---
role_id: services-mod
posture: mod
contract_posture: producer
---

# Role: services-mod

Patch/enhance/fix existing service code.

## layers_in_scope
`api`, `service`

## agents_in_scope / agents_skipped
Activates the `*-mod`-only conditional agents: `role-feasibility-pass1`,
`role-feasibility-pass2`, `git-history-capture` (if also
`incident-fix`), `refactor-migration` (if the change reshapes existing
contracts).

## contract_posture
`producer` (may amend the existing contract; `api-contract` diffs
against the prior version rather than authoring fresh).

## Used By
The brownfield scenario — enhancing/refactoring/fixing the already-built
URL shortener.
