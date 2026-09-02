# Platform Catalog

| Platform | Effect |
|---|---|
| `none` (default) | No cloud overlay. This project runs entirely local for the assignment's 2-3 day scope. |
| `gcp` | Cloud Run, GCS+CDN, Firestore, Pub/Sub, IAM (skeleton only — not exercised in this project) |
| `aws` | ECS Fargate, S3+CloudFront, DynamoDB, SNS+SQS (skeleton only) |
| `azure` | Container Apps, Cosmos DB, Service Bus, Key Vault (skeleton only) |

MCP integrations actually enabled for this project: **code-graph only**
(local, no external account needed). Jira/Figma/Playwright configs are
scaffolded in `config/mcp/` but inactive — there is no live Jira/Figma
workspace for this assignment.
