# Rule: Quality Gates

**Category:** Quality · **Priority:** 3

Defines the pass/fail criteria enforced at each of the 8 HITL gates and
the severity-band logic used by `build-verdict` and `grading-feedback`.

## Severity Bands (STEP-4.1 Build-Green)
| Band | Examples | Verdict |
|---|---|---|
| BLOCKER | compile error, unresolved symbol | always FAIL |
| HIGH | significant warnings on in-scope files | FAIL |
| MEDIUM/LOW | style nits, non-blocking warnings | report only, no gate |

## Final Grading (STEP-6.3)
`grading-feedback` produces a score in `[0.0, 1.0]`. **PASS** requires
`score >= 0.8` AND zero open BLOCKER/HIGH findings. Anything else is
**FAIL**, routing back to STEP-4 or, if retries are exhausted, to
Gate 4 for an operator waiver decision.

## Gate Enforcement Is Config-Driven
Which gates are active/optional/auto in a given run is declared by the
active `modes/{mode}/mode-manifest.md`, resolved into
`_role-context.yaml.gate_thresholds` by `role-resolver`. See
`rules/quality-policies.md` for the three-layer decoupling model.
