package com.aisdlc.urlshortener.api.dto;

import java.util.List;

/**
 * Response body for {@code POST /api/v1/links/batch} (feature-spec.md Section 3.1). Always
 * returned with {@code 200 OK} once whole-request validation passes -- {@code results} has
 * exactly {@code items.length} entries in request order, and
 * {@code successCount + failureCount == results.size()} always.
 */
public record BatchCreateLinkResponse(List<BatchItemResult> results, int successCount, int failureCount) {
}
