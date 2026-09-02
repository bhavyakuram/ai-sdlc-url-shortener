# UX Prototype — Scope Note

**Interpretation decision** (flagged per `rules/architecture.md` Proof
Over Promise — stating this rather than silently reinterpreting):
`stacks/java-spring/stack-manifest.md` declares this project API-only
(no `frontend` layer). `ux-prototype` still fires because
`role=greenfield` unconditionally triggers STEP-0 per CLAUDE.md's Phase
Pipeline. Since there is no real frontend client in scope, this
prototype is a **thin reference UI** — a self-contained mockup showing
how *any* future client (or a developer exercising the API by hand)
would interact with the four Must/Should-have flows from `concept.md`.
It is illustrative only; it is not part of the api/service/data
layers `generator` will build in STEP-4.

## Screens (4 of the allowed max 12)
1. `index.html` — create a short link (long URL + optional alias/expiry)
2. `created.html` — the resulting short link
3. `analytics.html` — click count + event log for a link
4. `error.html` — expired / not-found short code

Zero external dependencies — inline CSS only, no CDN/script includes.
