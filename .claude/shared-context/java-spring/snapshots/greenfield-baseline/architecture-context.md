---
snapshot_sha: greenfield-baseline
stack: java-spring
generated_by: architecture-analysis (PRE-WORK)
---

# Architecture Context: java-spring (clean-slate baseline)

## Finding
No existing `java-spring` service code exists in this repository. This
is a genuine greenfield baseline — `feature_shape=greenfield-app`.

## Baseline Declared
- Layers: `api`, `service`, `data` (per `stacks/java-spring/stack-manifest.md`)
- Package root: `com.aisdlc.urlshortener`
- Build: Maven, single module
- No prior architectural decisions to reconcile against — `generator`'s
  first wave will lay down the scaffold per `rules/greenfield-scaffold.md`.

## Drift-vs-Standards
N/A (nothing to drift from yet). Will be populated on the first
brownfield run against this codebase.
