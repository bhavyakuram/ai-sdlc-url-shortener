# Rule: MCP Convention

**Category:** Integration · **Priority:** 3

## Transparent Mediation Invariant
Agents **NEVER** call an MCP server directly. In local/interactive runs,
Claude Code hooks intercept every `mcp_*` call
(`hooks/mcp-interceptor/pre-tool-use.py` / `post-tool-use.py`); in CI,
a snapshotter skill performs the equivalent mediation. Both paths
enforce: write-once snapshots, per-family TTLs, auth-token scrubbing,
and isolated retries (an MCP failure never silently retries against a
different cached response).

## Per-Family Config
`config/mcp/{family}-config.yaml` declares TTL and connection details
per family (jira, figma, playwright, code-graph — see
`reference/platform-catalog.md` for the URL-shortener project's actual
enabled set, which is code-graph only; Jira/Figma/Playwright configs
are scaffolded but inactive since this assignment has no live
Jira/Figma workspace).
