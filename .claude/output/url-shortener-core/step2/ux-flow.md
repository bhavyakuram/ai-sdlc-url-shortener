---
agent: ux-design
inputs: [step2/feature-spec.md]
figma_export: none (no live Figma workspace for this project — see rules/mcp-convention.md)
---

# UX / Interaction Flow

**Scope note** (consistent with the Gate-0.5-approved framing): since
`java-spring` is API-only, "UX" here means the *API interaction
sequence* a client follows — not a screen flow. The Gate-0.5-approved
`step0/ux-prototype/` mockup screens map 1:1 onto these sequences:

1. **Create flow** (`index.html` → `created.html`) =
   `POST /links` → `201` with the new short link.
2. **Redirect flow** (implicit — a browser navigating, not a mockup
   screen) = `GET /{code}` → `302` to target, or `404`/`410`.
3. **Analytics flow** (`analytics.html`) =
   `GET /links/{code}/analytics` → click count + event log.
4. **Error flow** (`error.html`) = `GET /{code}` on an expired code →
   `410 Gone`.

## Interaction Rules
- Alias conflict (`409`) must return a machine-readable `code` field
  (e.g. `ALIAS_TAKEN`) distinct from the human `message`, so a real
  future client can react programmatically without string-matching.
- The distinction between `404` (never existed) and `410` (expired) is
  intentional and must be preserved end-to-end — this is the one place
  this flow refines idea.md's ask rather than just restating it.
