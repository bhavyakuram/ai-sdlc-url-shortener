-- Greenfield initial schema, verbatim from state-migration.md (STEP-3), with
-- IF NOT EXISTS added so restarting the app against the same H2 file (durability,
-- FR-5) does not fail on a second CREATE TABLE. Additive-only, per
-- rules/data-layer.md Migration Safety.

CREATE TABLE IF NOT EXISTS short_link (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(32) NOT NULL UNIQUE,
  target_url VARCHAR(2048) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  expires_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS click_event (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  short_link_id BIGINT NOT NULL REFERENCES short_link(id),
  occurred_at TIMESTAMP NOT NULL,
  referrer VARCHAR(512) NULL,
  country VARCHAR(2) NULL
);

CREATE INDEX IF NOT EXISTS idx_click_event_short_link_id ON click_event(short_link_id);
