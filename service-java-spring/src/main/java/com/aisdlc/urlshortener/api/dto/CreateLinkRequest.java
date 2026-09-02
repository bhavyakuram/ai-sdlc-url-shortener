package com.aisdlc.urlshortener.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/** Request body for POST /links. See step3/api-contract.yaml. */
public record CreateLinkRequest(

        @NotBlank(message = "targetUrl is required")
        @Pattern(regexp = "^https?://.+", message = "targetUrl must be an absolute http(s) URL")
        String targetUrl,

        @Pattern(regexp = "^[a-zA-Z0-9_-]{3,32}$", message = "alias must be 3-32 chars of [a-zA-Z0-9_-]")
        String alias,

        @Positive(message = "expiresInDays must be positive")
        Integer expiresInDays
) {
}
