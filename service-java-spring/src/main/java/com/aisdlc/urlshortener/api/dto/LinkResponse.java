package com.aisdlc.urlshortener.api.dto;

import com.aisdlc.urlshortener.data.ShortLinkEntity;

import java.time.Instant;

/** Response body for {@code POST /api/v1/links} (feature-spec.md 3.1). */
public record LinkResponse(String shortCode, String shortUrl, String longUrl, Instant createdAt, Instant expiresAt) {

    public static LinkResponse from(ShortLinkEntity link, String shortUrl) {
        return new LinkResponse(link.getCode(), shortUrl, link.getTargetUrl(), link.getCreatedAt(), link.getExpiresAt());
    }
}
