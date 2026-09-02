# Rule: Architecture

**Category:** Architecture · **Priority:** 3 (universal framework rule)

## Layer Boundaries
A run's active role declares `layers_in_scope` (e.g. `api`, `service`,
`data`). Agents and the code they generate MUST NOT read or write outside
those layers. A `services-mod` role touching the HTTP-handler layer is a
BLOCKER finding at STEP-6, not a warning.

## Dependency Direction
`api` -> `service` -> `data`. No layer may import from a layer above it
(the data layer must not know about HTTP concerns; the API layer must not
embed persistence logic). This is checked mechanically by static-analysis
(STEP-6.1), not just asserted by generator.

## Technology Agnosticism
No agent or skill file may hardcode a stack name, file extension, or
grep pattern. All stack-specific knowledge (language, framework, file
layout) is injected at runtime from `stacks/{active-stack}/` — standards
to OBEY, reference code to IMITATE, and `stack-skills.yaml` recipes to
invoke by id. A rule or skill that fails this test — e.g. a hardcoded
`.java` extension check — must be rewritten as a recipe lookup.

## Proof Over Promise
Every factual claim an agent makes ("the file compiles", "the test
passes", "the endpoint exists") must be backed by a verbatim tool-call
output in that agent's report. "I verified it" without the Bash/Read
output attached is treated as unverified and fails build-verdict /
evaluator review.

## Write-Once Immutability
Once a phase's Gate has passed, its output files under
`.claude/output/{feature-id}/stepN/` are read-only for the remainder of
the run. Only three sanctioned exceptions may append to a closed phase:
Gate 5 standards waivers, Gate 6 primitive exclusions, and contract-delta
amendments explicitly approved at a later gate.
