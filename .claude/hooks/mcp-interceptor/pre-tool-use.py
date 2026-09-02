#!/usr/bin/env python3
"""
Pre-tool-use hook: intercepts every mcp_* call before it reaches the
network. Checks the write-once snapshot cache; on a hit, serves the
cached response (exit code 2, per Claude Code hook convention: block
the real call, substitute this output). On a miss, allows the live
call through (exit code 0) — post-tool-use.py captures its response.

TODO(scaffold): wire to actual cache-config.json TTL/scope rules once
a real MCP family (currently only code-graph is enabled for this
project) needs interception. stdlib only — json, os, sys, re,
fnmatch, hashlib, tempfile, pathlib, datetime — per
rules/mcp-convention.md and Section 14.3 of docs/architecture-overview.md.
"""
import json
import sys


def main() -> int:
    payload = json.load(sys.stdin)
    tool_name = payload.get("tool_name", "")
    if not tool_name.startswith("mcp_"):
        return 0  # not an MCP call, nothing to mediate
    # TODO: hash args, check shared-context/{stack}/snapshots cache,
    # serve cached response on hit (exit 2), else allow through (exit 0)
    return 0


if __name__ == "__main__":
    sys.exit(main())
