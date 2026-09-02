---
agent: planner
---

# Sprint Plan — R1 Fix

Single wave, one file modified:
1. `service/LinkService.java` — wrap the click-write in try/catch, add SLF4J logger.

No new files. No DTO/controller changes — FS-6 doesn't touch the API
contract at all (same `302`, same headers).
