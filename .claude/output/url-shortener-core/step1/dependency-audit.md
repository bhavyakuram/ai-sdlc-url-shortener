---
agent: dependency-audit
---

# Dependency Audit

New dependencies required (none exist yet — greenfield):
| Dependency | Purpose | Risk |
|---|---|---|
| `spring-boot-starter-web` | REST API | none — core, well-maintained |
| `spring-boot-starter-data-jpa` | persistence | none |
| `com.h2database:h2` | in-memory DB (prototype default) | none for prototype; flagged for swap to Postgres before any real production use |
| `spring-boot-starter-validation` | request validation (security.md Input Validation) | none |
| `spring-boot-starter-test` + JUnit 5 (test scope) | testing | none |

No CVE-flagged versions selected (spot-checked against current stable
releases at time of writing — `security-audit` at STEP-6 re-verifies
mechanically, this is not the final check).
