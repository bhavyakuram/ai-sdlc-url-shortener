---
stack_id: java-spring
layers: [api, service, data]
capabilities: [api:rest, services:jvm, data:relational, build:maven, test:junit]
allowed_modes: [deterministic, hybrid, agentic]
---

# Stack: java-spring

## Identity
Java 21 + Spring Boot 3 REST service. Maven build. H2 (in-memory) for
the prototype default, swappable to Postgres via `db-harness/` per
`rules/data-layer.md`.

## Versions
- Java 21 (LTS)
- Spring Boot 3.3.x
- Maven 3.9.x
- JUnit 5 + Mockito for `test:junit`

## Layers
| Layer | Package | Notes |
|---|---|---|
| api | `com.aisdlc.urlshortener.api` | `@RestController` classes, request/response DTOs |
| service | `com.aisdlc.urlshortener.service` | business logic, no HTTP concerns |
| data | `com.aisdlc.urlshortener.data` | Spring Data JPA repositories |

## Capabilities Declared
`api:rest`, `services:jvm`, `data:relational`, `build:maven`,
`test:junit`. `generator` and `build-verdict` cross-check these against
what each agent declares it requires at `stack-validator` startup
(Section 2.1, closed vocabulary of 36 tokens).

## quality_policies
```yaml
quality_policies:
  code_reuse: {enabled: true, active_postures: [mod]}
  refactor_completeness: {enabled: false}
  primitive_exclusions: {enabled: false}
```

## Recipes (`stack-skills.yaml`)
`build`: `mvn -q -DskipTests compile`
`test`: `mvn -q test`
`test_coverage`: `mvn -q jacoco:report`
`lint`: `mvn -q spotbugs:check checkstyle:check`
