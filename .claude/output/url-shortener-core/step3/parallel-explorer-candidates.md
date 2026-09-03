---
skill: parallel-explorer
---

# Parallel Exploration: Short-Code Generation Strategy

3 real, independent `Agent`-tool dispatches (54-55k tokens each, run
concurrently) — same fork explored once before for this feature-id,
re-derived fresh for this independent trigger, not reused.

## Candidate A — Sequential ID Encoding
Insert to get a DB-assigned id → base62-encode → update the row's
code, same transaction. **Satisfies AC10 structurally**: the DB
sequence/identity generator is atomic by construction, so two
concurrent generated-code requests can never receive the same id.
**Downsides**: 2 writes per creation (extra WAL pressure under H2
file-mode's fsync durability requirement — a cost specific to *this*
run's A4 decision that wasn't as sharp a concern in the abstract);
brief null-code window between insert and update; sequential codes
leak creation order/volume.

## Candidate B — Random Token + Retry
Random 7-char base62, insert, catch constraint violation, retry
bounded. **Satisfies AC10 probabilistically + a deterministic
fallback**: DB unique index is the single serialization point: one
commits, one throws, the loser retries. Explicit retry-exhaustion
path required (503 + log, never a silent 500). **Downsides**: wasted
round-trips on collision (rare but real), latency-tail
non-determinism, provider-specific exception-type matching needed.

## Candidate C — UUID/Hash-Derived Token
**Self-identified as NOT satisfying AC10 as literally specified.**
This run's exploration went further than last time's: at 7 base62
chars (~3.5×10^12 space), collision probability per insert against a
10M-row corpus is ~0.0003% — non-zero, and AC10 requires it be
handled every time, not just be rare. "Insert directly, no pre-check"
as literally proposed either silently overwrites (lost write) or
throws unhandled (500) on that rare collision — a **flat AC10
violation**, not a trade-off. Made to comply only by bolting on the
same unique-constraint-and-retry mechanism Candidate B already is —
at which point it's not really a distinct candidate anymore, just
Candidate B with UUID as the token source. Also flags a
correctness bug if hash-derived (not random) tokens were used:
identical URLs submitted twice would deterministically collide.

## Conductor's Merge Decision
**Selected: Candidate A.** Same choice as the prior exploration of
this fork, arrived at independently rather than carried over:
- AC10 is a hard requirement. Candidate A satisfies it *structurally*;
  Candidate C's own analysis this run explicitly concluded it does
  NOT satisfy AC10 without becoming Candidate B; Candidate B satisfies
  it *probabilistically-with-a-deterministic-fallback*, which is
  correct but strictly more moving parts for no benefit this
  requirement needs.
- Candidate A's downsides (2 writes, order-leakage) are accepted —
  same reasoning as before: no requirement asks for code
  unguessability or hidden volume, and the write-latency cost is
  within the p95<50ms NFR target (`step0/concept.md` Section 5) for a
  single-instance H2 prototype.
- Candidate C is rejected outright — the strongest rejection yet,
  since this run's exploration produced an explicit probability
  calculation showing it fails the hard AC, not just "has downsides."
- Candidate B kept as documented alternative, not implemented.
