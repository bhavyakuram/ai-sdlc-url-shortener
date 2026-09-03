package com.aisdlc.urlshortener.api.dto;

import org.springframework.http.HttpStatus;

import java.time.Instant;

/** Standard error envelope for every 4xx/5xx response (feature-spec.md Section 1). */
public record ErrorResponse(Instant timestamp, int status, String error, String code, String message) {

    public static ErrorResponse of(HttpStatus status, String code, String message) {
        return new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), code, message);
    }
}
