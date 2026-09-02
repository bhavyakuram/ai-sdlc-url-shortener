# Stack Catalog

| Stack | Frontend | Services | Data | API |
|---|---|---|---|---|
| `java-spring` | — (API-only) | Java 21 / Spring Boot 3 | H2 / Postgres (via db-harness) | REST (OpenAPI 3.1) |
| `python-fastapi` | — (API-only) | Python 3.12 / FastAPI | SQLite / Postgres (via db-harness) | REST (OpenAPI 3.1) |

Capabilities vocabulary in use: `api:rest`, `services:jvm`,
`services:asgi`, `data:relational`, `build:maven`, `build:pip`,
`test:junit`, `test:pytest`. Selecting a stack is a launch-time
argument (`/run-sdlc <stack> ...`); it is never a compiled-in default,
per the project's dynamic-language requirement.
