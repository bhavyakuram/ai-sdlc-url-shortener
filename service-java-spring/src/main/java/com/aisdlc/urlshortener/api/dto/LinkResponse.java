package com.aisdlc.urlshortener.api.dto;

import com.aisdlc.urlshortener.data.ShortLinkEntity;

import java.time.Instant;

/** Response body for a created/looked-up short link. */
public record LinkResponse(String code, String targetUrl, Instant expiresAt) {

    public static LinkResponse from(ShortLinkEntity entity) {
        return new LinkResponse(entity.getShortCode(), entity.getTargetUrl(), entity.getExpiresAt());
    }
}
