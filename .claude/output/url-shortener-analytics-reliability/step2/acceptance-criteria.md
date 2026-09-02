---
agent: acceptance-criteria
---

# Acceptance Criteria

## AC-15 (FS-6) Redirect succeeds even if click-recording throws
GIVEN an existing, unexpired short code,
WHEN `GET /{code}` is called AND the click-event write throws an
exception,
THEN the caller still receives a `302` to the target URL, and the
failure is logged.

## AC-16 (FS-6, regression) Normal path unchanged
GIVEN an existing, unexpired short code and a healthy DB,
WHEN `GET /{code}` is called,
THEN behavior is identical to AC-5 — `302` returned AND the click IS
recorded (this fix must not turn click recording into a no-op).
