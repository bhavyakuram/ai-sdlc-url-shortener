# Idea: URL Shortener Service

We need a URL shortener service, built from scratch. Core idea:

- Someone submits a long URL and gets back a short link.
- Visiting the short link redirects to the original long URL.
- We want to know how the short links are performing — clicks, when
  they happen, roughly where traffic is coming from.
- It needs to hold up under real usage: don't lose data, don't let two
  requests collide on the same short code, don't fall over if a bad
  actor hammers one link.
- Would be nice if people could pick their own short code instead of a
  random one, if it's free.
- Links probably shouldn't live forever — some kind of expiry would be
  good, though we haven't nailed down the exact rule.
- No login/accounts needed for v1 — anonymous is fine.

That's the shape of it. Not sure yet whether expiry should be a fixed
TTL for everyone or configurable per-link, and analytics could mean
anything from "just a counter" to "full click log" — figure out what's
reasonable for a first version and flag anything that needs a decision.
