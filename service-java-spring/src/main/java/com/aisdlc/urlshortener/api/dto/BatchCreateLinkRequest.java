package com.aisdlc.urlshortener.api.dto;

import java.util.List;

/**
 * Request body for {@code POST /api/v1/links/batch} (feature-spec.md Section 3.1). Reuses
 * {@link CreateLinkRequest}{@code (url, customCode)} verbatim for each item -- Section 0's
 * mandated vocabulary reuse, and zero new field-shape code for the per-item request.
 */
public record BatchCreateLinkRequest(List<CreateLinkRequest> items) {
}
