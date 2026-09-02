# Rule: Prompt Caching

**Category:** Optimization · **Priority:** 3

## Canonical Shared Context Block
`sdlc-launcher` composes a canonical shared-context block, in this
strict byte-for-byte identical order, prepended to every agent dispatch
within a run:

1. `CLAUDE.md`
2. All active rule files, alphabetical order
3. The active role manifest
4. The active skill's `SKILL.md`

Because this block is byte-identical across every dispatch in a run, it
is served from Anthropic's prompt cache (5-minute TTL) instead of being
re-processed per agent — this is the framework's primary cost lever,
not a nice-to-have.

## What Breaks the Cache
Anything that makes the block non-identical between dispatches within
the same run: reordering rule files, conditionally including a rule
based on phase (don't — gate the *behavior* inside the rule text
instead), or interpolating per-agent values into the shared block.
