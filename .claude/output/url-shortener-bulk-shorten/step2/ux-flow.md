---
agent: ux-design
---

# Interaction Flow: Bulk Shorten

Single new flow: submit a list, get back a list of the same length in
the same order, where each slot independently says "created" (with
the link) or "error" (with the same machine-readable error code the
single-create endpoint would have used). A caller can zip the request
items with the response results by index to know exactly which one
failed and why — no separate lookup needed.
