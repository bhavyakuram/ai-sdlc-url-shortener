# Role Catalog

| Role | Posture | Layers in scope | Contract posture | Used by |
|---|---|---|---|---|
| `fullstack` (default) | dev | api, service, data | producer | greenfield scenario |
| `services-mod` | mod | api, service | producer | brownfield scenario |
| `services-doc` | doc (audit-only) | api, service (read-only) | consumer | test/doc-improvement scope |
| `greenfield` | greenfield | api, service, data | producer | very first run, triggers STEP-0 |

See `rules/role-combination-matrix.yaml` for valid/invalid combinations.
