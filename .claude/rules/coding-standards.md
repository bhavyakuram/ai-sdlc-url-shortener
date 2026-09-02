# Rule: Coding Standards

**Category:** Quality · **Priority:** 3

## Naming
Names are descriptive and consistent with the active stack's
`standards/naming.md`. No single-letter identifiers outside tight loop
counters. No abbreviations that aren't already idiomatic in the stack
(`req`/`res` fine; `usrSvcImpl2` not fine).

## No Dead Code
`generator` must not leave commented-out code, unused imports, or
unreferenced private methods. `code-reuse.md` enforcement (when active
via `quality_policies.code_reuse.enabled`) additionally requires
checking for an existing equivalent before writing a new helper.

## Logging
Every service-layer method that can fail logs the failure with enough
context to diagnose without re-running the request (stack-scoped log
format — see `stacks/{active-stack}/standards/`). No `print`/`System.out`
in production code paths.

## No Silent Catches
`catch`/`except` blocks must either handle the error meaningfully or
re-raise with added context. An empty catch block is a BLOCKER finding.
