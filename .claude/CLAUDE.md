# CLAUDE.md — Master Orchestration Rules

**Priority 2 in the rule hierarchy** (below explicit user instruction,
above everything else — see the "Rule Priority Hierarchy" list below).

## What This Framework Is
An agentic, multi-phase SDLC pipeline that takes a feature (greenfield,
brownfield, or ambiguous) from raw requirement to production-ready,
graded code — for the URL Shortener project — using AI agents,
human-in-the-loop gates, and a configurable technology stack
(`java-spring` or `python-fastapi`, see `stacks/`).

## Entry Point
```
/sdlc-launcher <stack> <feature> [role] [--mode=deterministic|hybrid|agentic] [--platform=none|aws|gcp|azure]
```
A real, invokable Claude Code skill — `skills/sdlc-launcher/SKILL.md`
(Claude Code auto-discovers every `.claude/skills/*/SKILL.md`, named
after its directory; no separate command registration needed). Do not
invoke phase agents directly — always go through the launcher so the
dependency graph, gates, retry tracking, and audit log stay consistent.

## The Four Configuration Axes
Resolved once at run start, frozen for the run's duration (mid-run
change of any axis is forbidden):
1. **Stack** (technology) — `java-spring` | `python-fastapi`
2. **Role** (scope) — `fullstack` | `services-mod` | `services-doc` |
   `greenfield`
3. **Mode** (execution posture) — `deterministic` | `hybrid` |
   `agentic` (default for this project — see `modes/agentic/mode-manifest.md`)
4. **Platform** (cloud overlay) — `none` (default) | `aws` | `gcp` |
   `azure`

## Phase Pipeline (see `docs/architecture-overview.md` for full detail)
`STEP-0 (greenfield only)` → `PRE-WORK` → `STEP-1 Discovery` →
`STEP-2 Spec & UX` → `STEP-3 Technical Design` → `STEP-4 Planning &
Implementation` ⇄ `STEP-5 Validation` ⇄ `STEP-6 Audit & Grading` →
`COMPLETE` (or `SAFE-STOPPED`, see `rules/retry-rollback-safestop.md`).

## Non-Negotiables
- Agents declare WHAT; skills implement HOW; rules enforce WHAT MUST
  hold true; stack context supplies technology-specific knowledge.
  Never blur these roles (`rules/architecture.md` Separation of
  Concerns).
- No agent, skill, or rule file may hardcode a stack name, file
  extension, or grep pattern.
- Every factual claim must be backed by a verbatim tool-call output
  (Proof Over Promise).
- A feature is COMPLETE only when `rules/quality-gates.md`'s
  Completion Criteria (Final Grading, STEP-6.3) all hold.

## Rule Priority Hierarchy
1. Explicit user instruction (highest — human always wins)
2. This file (master orchestration)
3. `.claude/rules/` (universal framework rules)
4. `stacks/{stack}/standards/` (stack-specific standards)
4.25. `platforms/{platform}/standards/` (platform-specific standards)
4.5. `roles/{role}/role-manifest.md` (role-scope manifest)
5. Agent definitions (`.md` files)
6. Skill instructions (`SKILL.md` files)
7. Model defaults (lowest)
