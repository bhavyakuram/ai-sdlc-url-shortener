# Rule: Greenfield Scaffold

**Category:** Greenfield · **Priority:** 3

## Scaffold Templates
When `feature_shape = greenfield-app`, `generator`'s first wave lays
down the stack's baseline scaffold (project file, entrypoint, health
endpoint, empty layer directories per `layers_in_scope`) before any
feature-specific code, using `stacks/{stack}/reference/scaffold/` as
the template.

## Greenfield KPI Baselining
`step0/concept.md`'s success metrics become the KPI baseline recorded
at completion (`_reliability-metrics.json`), so a later brownfield run
against this same codebase has a "day zero" number to compare against.

## Build-Green Exception
The first `build-verdict` run against a greenfield scaffold has no
prior index to diff against — it writes `GREENFIELD_SCAFFOLD_PASS`
rather than a normal diagnostic verdict (see `rules/build-green.md`).
