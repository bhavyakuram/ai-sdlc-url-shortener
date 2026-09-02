# Rule: Build Green

**Category:** Quality · **Priority:** 3

## The IDE/Compiler Is the Build Oracle
`build-verdict` (STEP-4.1) never asserts "the code compiles" from
reading source — it must invoke the real toolchain (`mvn -q compile` /
`gradle compileJava` for `java-spring`; `python -m py_compile` +
`mypy --strict` for `python-fastapi`) and parse the actual diagnostics.

## Special Cases
- **Greenfield first run:** nothing has been indexed yet — writes
  `GREENFIELD_SCAFFOLD_PASS` instead of a diagnostic-based verdict.
- **No toolchain available:** STOP with an explicit prompt to the
  operator. This is never a silent skip.
- **`--build-verdict=skip`:** allowed per-run but the run is marked
  degraded — `parallel-explorer` and `adaptive-gate` budgets are
  reduced accordingly, and this is recorded in `_run-log.md`.

## Routing
PASS or PASS_WITH_WARNING proceeds to STEP-5. FAIL routes back to
`generator` with the diagnostic output attached verbatim.
