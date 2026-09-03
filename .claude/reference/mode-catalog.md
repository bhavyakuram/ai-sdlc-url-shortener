# Mode Catalog — 8-Gate x 3-Mode Matrix

| Gate | Position | Purpose | Deterministic | Hybrid | Agentic |
|---|---|---|---|---|---|
| 0 | After concept + market-research | Approve greenfield concept | active | active | active |
| 0.5 | After ux-prototype | Approve prototype | active if fired | active if fired | active if fired |
| 1 | After STEP-1 | Ratify GO/NO-GO verdict | active | active | active |
| 2 | After STEP-2 | Spec freeze (PO sign-off) | active | active | active |
| 3 | After STEP-3 | Design freeze (architect sign-off) | active | active (skippable, consumer-only) | optional |
| 4 | On STEP-6 FAIL | Risk-accepted release waiver | active | active | auto after 10 consistent approvals |
| 5 | STEP-3 prereq missing | Standards prerequisite decision | active | active | auto after 10 consistent approvals |
| 6 | Gate 3 extension | Primitive exclusion veto | active | active (never auto) | active (never auto) |

All gates use the `AskUserQuestion` mechanism and capture decisions in
structured YAML for reproducibility and audit (`rules/mode-policy.md`).

**Gate 0.5 is conditional on `ux-prototype` actually firing**
(`agents/step0/ux-prototype.md`: only when the active stack declares a
`frontend` layer). An API/service-only stack never fires
`ux-prototype`, so Gate 0.5 is skipped entirely for such a run — there
is nothing to approve, not a silently-auto-approved gate.
