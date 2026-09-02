# Rule: Standards Path Convention

**Category:** Architecture · **Priority:** 3

## How Standards Override Codebase Patterns
When `reference/` sample code and the actual codebase disagree, the
organization-authored `standards/` files win — reference code is "what
good looks like," standards are "what MUST be true." `codebase-context`
flags any drift between the two as a non-blocking observation, not an
error, unless a specific standard is marked `enforced: strict` in the
stack manifest.

## Resolution Order
`stacks/{stack}/standards/` (per-layer) inherits from
`stacks/_shared/{java|graphql-dgs|...}/` cross-cutting standards, which
in turn sit below `rules/` (universal) and above `roles/{role}/`
role-scope manifests in the priority hierarchy (Section 8).
