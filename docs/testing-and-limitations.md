# Testing Approach, Limitations & Trade-offs

*(Deliverable. To be filled in as the framework is actually run against
the URL-shortener feature set — this is a structural placeholder for
now.)*

## Testing Approach
- **Framework self-test**: each phase's Gate is exercised at least once
  across the three required scenarios (greenfield / brownfield /
  ambiguous) before the URL shortener is considered proven out.
- **Product tests**: written by `test-generation` + `coverage-edge`
  (STEP-5) against every acceptance criterion from STEP-2, run via the
  active stack's `test` recipe (`mvn test` / `pytest -q`).
- **Coverage floor**: per `.claude/rules/testing.md` (60% smoke / 80%
  full / 80%+95% risk-weighted, chosen by `coverage-strategy`).

## Known Limitations
- Rollback reverts file-level changes; it does not yet reason about a
  completed, applied database migration (see `state-migration`
  boundary in `.claude/rules/data-layer.md`).
- `agentic` mode is a stretch demonstration, not the primary path.
- MCP integrations beyond `code-graph` are scaffolded but inactive.
- The two stacks (`java-spring`, `python-fastapi`) are believed
  feature-equivalent by construction (same agents/skills/rules drive
  both) but have not yet been diffed post-generation to confirm.

## Trade-offs
- Chose Markdown/YAML-as-the-framework over a coded orchestration
  engine, trading some type-safety for full auditability and zero
  build step (see `docs/architecture-overview.md` Section 9).
- Reduced the source framework's 28-role / 7-stack catalog to 5 roles /
  2 stacks — full mechanism, smaller instance space — appropriate for
  a single-service assignment rather than a multi-client program.
