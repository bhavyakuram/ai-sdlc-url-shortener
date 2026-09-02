---
agent: feasibility
---

# Feasibility / Audit Findings

**Investigation method**: code reading only, per `services-doc`'s
`layers_in_scope: [api, service]` (read-only) — no test code written,
consistent with that role skipping `test-generation`/`generator`.

**Verbatim evidence** (`LinkService.java:83-95`):
```java
@Transactional
public String resolveAndRecordClick(String code, String referrer) {
    ShortLinkEntity link = shortLinkRepository.findByShortCode(code)
            .orElseThrow(() -> new LinkNotFoundException(code));
    Instant now = Instant.now();
    if (link.isExpired(now)) {
        throw new LinkExpiredException(code);
    }
    clickEventRepository.save(new ClickEventEntity(link.getId(), now, referrer));
    return link.getTargetUrl();
}
```

## Finding on Ambiguity (a) — Lost writes under concurrent load
**No silent-loss mechanism exists.** There is no `try/catch` around
the `save()` call, and `click_event` has no unique constraint (only a
non-unique index — `step3/state-migration.md` from `url-shortener-core`)
to violate. A DB error here would propagate as an uncaught exception
→ an HTTP 500 to the caller. That's a *visible* failure, not silent
data loss. **The specific fear in the raw request ("quietly losing
data") does not match what the code can actually do.**

## Finding on Ambiguity (b) — Read-after-write consistency
`getAnalytics` and `resolveAndRecordClick` are separate `@Transactional`
methods against the same H2/Hibernate session-per-request model; a
click committed in one request is visible to a `getAnalytics` call in
a subsequent request (no caching layer sits between them). No
consistency gap found by code reading.

## Overall Verdict
**No confirmed bug.** The concern in the raw request doesn't hold up
against the actual code. This itself is a valid, useful audit outcome
— "we checked, here's why it's fine" is not a non-answer.
