# Rule: Reliability Metrics

**Category:** Optimization · **Priority:** 3
**Status: NEW — added to satisfy the assignment's explicit "success
rate, retry/rollback frequency, MTTR, and end-to-end latency" tracking
requirement. The source framework's `_token-telemetry.json` tracks cost
only; this file adds the reliability dimension alongside it using the
same per-run, per-feature file convention.**

## `_reliability-metrics.json`
Written by `sdlc-launcher` at every phase transition (see Section 11,
Launcher Orchestration Sequence, step 12 "Update telemetry" — extended
to also write this file). Schema:

```json
{
  "feature_id": "string",
  "phase_events": [
    {"phase": "STEP-1", "entered_at": "iso8601", "exited_at": "iso8601",
     "verdict": "PASS|FAIL", "retries_consumed": 0, "rolled_back": false}
  ],
  "success_rate": "PASS phases / total phase attempts, this run",
  "mttr_seconds": "mean time from a FAIL verdict to the next PASS verdict for the same phase, this run",
  "end_to_end_latency_seconds": "wall-clock from PRE-WORK entry to COMPLETE or SAFE-STOPPED",
  "retry_frequency": "retries_consumed / total phase attempts",
  "rollback_frequency": "rolled_back=true count / total phase attempts"
}
```

## Cross-Run Rollup
`pattern-extractor` additionally rolls these metrics up into
`run-history/_online-learning.yaml` per stack+role matrix row, so
`adaptive-gate` and `online-learning` have real historical MTTR/success
data to calibrate against — not just pass/fail counts.

## Reporting
The Final Engineering Summary deliverable pulls its reliability section
directly from this file, not from a hand-written estimate.
