# Rule: Input Path Convention

**Category:** Process · **Priority:** 3

All human-curated feature inputs live under
`.claude/inputs/{feature-id}/`:

```
inputs/{feature-id}/
├── jira/              <- Jira ticket exports (mcp:jira)
├── ideation/          <- idea.md for greenfield runs
└── supporting-docs/   <- anything else the operator hands in
```

`requirement-ingestion` (PRE-WORK) is the only agent that reads directly
from this tree; everything downstream reads its normalized
`prework/prd-v0.md` output instead. This keeps a single ingestion point
so the rest of the pipeline never re-parses raw operator input.
