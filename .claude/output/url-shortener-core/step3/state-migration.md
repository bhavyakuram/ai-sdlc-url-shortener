---
agent: state-migration
---

Greenfield initial schema (H2 file-mode per A4):
```sql
CREATE TABLE short_link (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(32) NOT NULL UNIQUE,
  target_url VARCHAR(2048) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  expires_at TIMESTAMP NOT NULL
);
CREATE TABLE click_event (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  short_link_id BIGINT NOT NULL REFERENCES short_link(id),
  occurred_at TIMESTAMP NOT NULL,
  referrer VARCHAR(512) NULL,
  country VARCHAR(2) NULL
);
CREATE INDEX idx_click_event_short_link_id ON click_event(short_link_id);
```
`country` added to `click_event` vs. the prior pass's schema — new
NFR from this run's concept.md A3/A2 decisions (country-level geo).
Additive, consistent with `rules/data-layer.md` Migration Safety.
