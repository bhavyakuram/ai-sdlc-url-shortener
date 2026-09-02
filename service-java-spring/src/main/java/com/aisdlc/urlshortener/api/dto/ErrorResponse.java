package com.aisdlc.urlshortener.api.dto;

/**
 * Machine-readable error shape. {@code code} is stable and meant for
 * programmatic handling (e.g. {@code ALIAS_TAKEN}); {@code message} is
 * for humans. See step2/ux-flow.md Interaction Rules.
 */
public record ErrorResponse(String code, String message) {
}
