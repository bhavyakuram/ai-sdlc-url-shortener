package com.aisdlc.urlshortener.api.dto;

/**
 * Request body for {@code POST /api/v1/links}. Field names match feature-spec.md Section 1
 * ({@code url}, {@code customCode}) -- the authoritative wire contract, since it specifies
 * exact JSON shapes and is cross-referenced by every AC. See generator-summary.md for the
 * discrepancy with the terser step3/api-contract.yaml stub, which uses different field
 * names ({@code targetUrl}, {@code code}).
 *
 * <p>No Bean Validation annotations here on purpose: every validation rule in FR-3/FR-8
 * needs its own distinct {@code code} vocabulary token in the error response
 * (INVALID_URL_SCHEME vs URL_TOO_LONG vs URL_MALFORMED, etc.), which {@link
 * com.aisdlc.urlshortener.service.LinkService}'s explicit checks provide and generic
 * {@code @NotBlank}-style annotations would not.
 */
public record CreateLinkRequest(String url, String customCode) {
}
