---
agent: planner
inputs: [step3/technical-design.md, step3/api-contract.yaml, step3/state-migration.md]
---

# Sprint Plan

Multi-wave dispatch (9 files across all layers — `generator`'s
"multi-wave for 5+ files" threshold applies):

## Wave 1 — Scaffold + Data Layer
1. `pom.xml`
2. `src/main/resources/application.yml`
3. `UrlShortenerApplication.java` (entry point)
4. `data/ShortLinkEntity.java`
5. `data/ClickEventEntity.java`
6. `data/ShortLinkRepository.java`
7. `data/ClickEventRepository.java`

## Wave 2 — Service Layer
8. `service/exception/AliasTakenException.java`
9. `service/exception/LinkNotFoundException.java`
10. `service/exception/LinkExpiredException.java`
11. `service/CodeGenerator.java`
12. `service/LinkService.java`

## Wave 3 — API Layer
13. `api/dto/CreateLinkRequest.java`
14. `api/dto/LinkResponse.java`
15. `api/dto/AnalyticsResponse.java`
16. `api/dto/ClickEventDto.java`
17. `api/dto/ErrorResponse.java`
18. `api/LinkController.java`
19. `api/ApiExceptionHandler.java`

Each wave builds on the prior wave's compiled output — Wave 2 assumes
Wave 1's entities/repos exist, Wave 3 assumes Wave 2's service exists.
`build-verdict` (STEP-4.1) runs after all waves land, per
`rules/build-green.md`.
