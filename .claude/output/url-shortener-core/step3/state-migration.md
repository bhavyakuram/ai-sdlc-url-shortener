---
agent: state-migration
---

# State Migration Plan

**Greenfield — no `db-harness/` configured, no prior schema.** This is
an initial creation, not a migration in the additive-vs-breaking sense
of `rules/data-layer.md`, but is still documented explicitly for
traceability.

## V1__init.sql (conceptual — Hibernate `ddl-auto=validate` against
Flyway/JPA-managed DDL in the generated service; H2 for the prototype)

```sql
CREATE TABLE short_link (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  short_code VARCHAR(32) NOT NULL UNIQUE,
  target_url VARCHAR(2048) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  expires_at TIMESTAMP NOT NULL
);

CREATE TABLE click_event (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  short_link_id BIGINT NOT NULL REFERENCES short_link(id),
  occurred_at TIMESTAMP NOT NULL,
  referrer VARCHAR(512) NULL
);

CREATE INDEX idx_click_event_short_link_id ON click_event(short_link_id);
```

The `UNIQUE` constraint on `short_code` is the concurrency mechanism
`technical-design.md` relies on for AC-9 (collision-safety) — this is
schema-enforced, not just application-logic-enforced.
