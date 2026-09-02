---
skill_id: transition-fsm
implements_agent: none (framework-level skill, no per-phase agent declaration)
---

# Skill: Transition FSM

## Purpose
Agentic mode: manages phase-to-phase state transitions within a matrix row (stack x role) as an explicit finite state machine

## Implementation Logic
States = {PREWORK, STEP0..STEP6, GATE_WAIT, RETRY, ROLLBACK, SAFE_STOP, COMPLETE}. Transitions are table-driven from build-verdict/evaluator/grading-feedback verdicts; illegal transitions are rejected and logged.

## Report Block
Logs its decisions to `.claude/output/{feature-id}/_run-log.md` and,
where it affects cross-run state, to `run-history/_online-learning.yaml`
or `.claude/output/{feature-id}/_token-telemetry.json` as applicable.
