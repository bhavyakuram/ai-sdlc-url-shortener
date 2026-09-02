# Cross-Cutting: Observability

**Engine-agnostic pattern**, inherited by both `java-spring` and
`python-fastapi` per `rules/standards-path.md`.

## Structured Logging
Every request logs: request id, method+path, status code, duration_ms.
No stack trace of an *expected* error (404, validation failure) at
ERROR level — those log at INFO/WARN.

## Health & Metrics Endpoints
`GET /healthz` — liveness. `GET /metrics` — exposes the reliability
counters this project's `generator` wires up per feature: total
requests, redirect count, 404 count, average redirect latency.

## Correlation
Every log line within a single request carries the same request id, so
a redirect failure can be traced end-to-end without guessing.
