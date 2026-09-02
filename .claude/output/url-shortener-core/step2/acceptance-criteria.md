---
agent: acceptance-criteria
inputs: [step2/feature-spec.md, step2/ux-flow.md]
---

# Acceptance Criteria

## AC-1 (FS-1) Create with generated code
GIVEN a valid `targetUrl` and no `alias`,
WHEN `POST /links` is called,
THEN a `201` is returned with a generated short code, the given
target, and an `expiresAt` 90 days out.

## AC-2 (FS-1) Create with custom alias
GIVEN a valid `targetUrl` and an unused `alias`,
WHEN `POST /links` is called,
THEN a `201` is returned using that exact alias as the short code.

## AC-3 (FS-1) Alias collision
GIVEN an `alias` that is already in use,
WHEN `POST /links` is called with that alias,
THEN a `409` is returned with `code=ALIAS_TAKEN`, and the existing
link is left unchanged.

## AC-4 (FS-1) Invalid target
GIVEN a `targetUrl` that is missing or not a valid absolute URL,
WHEN `POST /links` is called,
THEN a `400` is returned and no `ShortLink` is created.

## AC-5 (FS-2) Successful redirect + click recorded
GIVEN an existing, unexpired short code,
WHEN `GET /{code}` is called,
THEN a `302` to the target is returned AND a `ClickEvent` is recorded
for that code with the current timestamp.

## AC-6 (FS-2) Unknown code
GIVEN a short code that was never created,
WHEN `GET /{code}` is called,
THEN a `404` is returned and no `ClickEvent` is recorded.

## AC-7 (FS-2) Expired code
GIVEN a short code whose `expiresAt` is in the past,
WHEN `GET /{code}` is called,
THEN a `410` is returned and no `ClickEvent` is recorded.

## AC-8 (FS-3) Analytics reflect recorded clicks
GIVEN a short code with N recorded `ClickEvent`s,
WHEN `GET /links/{code}/analytics` is called,
THEN the response reports a count of exactly N and an event log with
N entries, each with a timestamp and a possibly-null referrer.

## AC-9 (R1 collision-safety, cross-cutting)
GIVEN two concurrent `POST /links` requests with no `alias` (generated
code path),
WHEN both are processed concurrently,
THEN both succeed with two distinct short codes — no collision, no
lost write, no 500.

Every AC above will have at least one test written by
`test-generation` (STEP-5), per `rules/testing.md` Every-AC-Needs-a-Test.
