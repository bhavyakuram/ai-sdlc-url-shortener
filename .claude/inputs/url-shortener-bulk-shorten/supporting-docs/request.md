# Enhancement Request: Bulk Shorten

Right now creating short links is one-at-a-time. We have a use case
where someone wants to shorten a whole list of URLs in one go instead
of calling the API N times. Add a way to submit multiple URLs and get
back multiple short links.

Didn't think through what should happen if one of the URLs in the
batch is bad (invalid, or the alias is already taken) — should the
whole batch fail, or just that one item? Also haven't picked a limit
on how many URLs can go in one batch. Use your judgment and flag it if
it matters.
