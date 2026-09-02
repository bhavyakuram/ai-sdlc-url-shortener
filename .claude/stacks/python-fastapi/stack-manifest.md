---
stack_id: python-fastapi
layers: [api, service, data]
capabilities: [api:rest, services:asgi, data:relational, build:pip, test:pytest]
allowed_modes: [deterministic, hybrid, agentic]
---

# Stack: python-fastapi

## Identity
Python 3.12 + FastAPI REST service, served by Uvicorn. SQLite (via
SQLAlchemy) for the prototype default, swappable to Postgres via
`db-harness/` per `rules/data-layer.md`.

## Versions
- Python 3.12
- FastAPI 0.115.x, Pydantic v2
- SQLAlchemy 2.x
- pytest + pytest-cov for `test:pytest`

## Layers
| Layer | Module | Notes |
|---|---|---|
| api | `app/api/` | FastAPI routers, Pydantic request/response models |
| service | `app/service/` | business logic, no HTTP/ASGI concerns |
| data | `app/data/` | SQLAlchemy models + repositories |

## Capabilities Declared
`api:rest`, `services:asgi`, `data:relational`, `build:pip`,
`test:pytest`.

## quality_policies
```yaml
quality_policies:
  code_reuse: {enabled: true, active_postures: [mod]}
  refactor_completeness: {enabled: false}
  primitive_exclusions: {enabled: false}
```

## Recipes (`stack-skills.yaml`)
`build`: `python -m py_compile app/**/*.py && mypy --strict app`
`test`: `pytest -q`
`test_coverage`: `pytest -q --cov=app --cov-report=term-missing`
`lint`: `ruff check app && ruff format --check app`
