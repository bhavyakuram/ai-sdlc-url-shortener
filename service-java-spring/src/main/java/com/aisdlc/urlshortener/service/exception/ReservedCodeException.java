package com.aisdlc.urlshortener.service.exception;

/**
 * {@code customCode} exactly matches a reserved top-level path segment (feature-spec.md
 * Section 5 -- {@code api}, {@code actuator}, {@code health}, {@code favicon.ico}). Maps to
 * {@code 400 Bad Request}, {@code code: RESERVED_CODE} -- deliberately distinct from
 * {@link CustomCodeTakenException}'s {@code 409 CUSTOM_CODE_TAKEN} both in HTTP status and
 * in error code, since "reserved, can never be taken" and "taken by another link" are
 * different conditions a client may want to branch on differently.
 */
public class ReservedCodeException extends RuntimeException {

    public ReservedCodeException(String message) {
        super(message);
    }
}
