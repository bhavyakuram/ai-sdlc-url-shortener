package com.aisdlc.urlshortener.service.exception;

/**
 * A valid, non-reserved {@code customCode} collided with an already-persisted link
 * (first-come-first-served, FR-8). Maps to {@code 409 Conflict}, {@code code:
 * CUSTOM_CODE_TAKEN}. Always raised from a caught {@code DataIntegrityViolationException}
 * on the DB unique constraint (insert-then-catch) -- never from a check-then-insert race,
 * per risk-register.md R-2's mitigation.
 */
public class CustomCodeTakenException extends RuntimeException {

    public CustomCodeTakenException(String message, Throwable cause) {
        super(message, cause);
    }
}
