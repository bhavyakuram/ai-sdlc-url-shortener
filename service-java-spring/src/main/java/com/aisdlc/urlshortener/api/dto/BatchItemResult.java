package com.aisdlc.urlshortener.api.dto;

import com.aisdlc.urlshortener.data.ShortLinkEntity;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Per-item discriminated result inside {@link BatchCreateLinkResponse#results()}
 * (feature-spec.md Section 3.1). {@code status: "CREATED"} carries {@code
 * shortCode}/{@code shortUrl}/{@code longUrl}/{@code createdAt}/{@code expiresAt} and omits
 * {@code code}/{@code message}; {@code status: "FAILED"} carries {@code code}/{@code message}
 * and omits the CREATED fields. {@code @JsonInclude(NON_NULL)} is applied at the class level
 * (not a global {@code ObjectMapper} change, which would touch every other endpoint's
 * serialization) so the two shapes are wire-level mutually exclusive, not merely nullable
 * (technical-design.md Section 5.3).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BatchItemResult(String status, String shortCode, String shortUrl, String longUrl,
                               Instant createdAt, Instant expiresAt, String code, String message) {

    public static BatchItemResult created(ShortLinkEntity link, String shortUrl) {
        return new BatchItemResult("CREATED", link.getCode(), shortUrl, link.getTargetUrl(),
                link.getCreatedAt(), link.getExpiresAt(), null, null);
    }

    public static BatchItemResult failed(String code, String message) {
        return new BatchItemResult("FAILED", null, null, null, null, null, code, message);
    }
}
