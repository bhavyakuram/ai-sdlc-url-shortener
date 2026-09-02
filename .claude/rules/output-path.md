# Rule: Output Path Convention

**Category:** Process · **Priority:** 3

All generated artifacts for a run live under
`.claude/output/{feature-id}/`:

```
output/{feature-id}/
├── step0/ .. step6/          <- per-phase artifacts (see each step's agent contracts)
├── _role-context.yaml        <- resolved role + policies + gate thresholds
├── _run-log.md               <- append-only audit log (START/END per agent dispatch)
├── _token-telemetry.json     <- cost tracking
└── _reliability-metrics.json <- phase timestamps, retry counts, gate wait times (NEW — see reliability-metrics.md)
```

No agent writes outside its own `stepN/` subfolder except the four
underscore-prefixed cross-cutting files, which have a single writer
each (see `rules/architecture.md` Write-Once Immutability and Section
11 Interconnectivity Map).
