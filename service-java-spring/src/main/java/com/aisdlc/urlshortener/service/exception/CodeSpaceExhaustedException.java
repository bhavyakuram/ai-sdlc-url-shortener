package com.aisdlc.urlshortener.service.exception;

/**
 * The server-generated-code path (Candidate A) could not allocate a unique code. Maps to
 * {@code 503 Service Unavailable}, {@code code: CODE_SPACE_EXHAUSTED}. Reserved for the
 * pathological, retry-budget-exhausted edge case (feature-spec.md 3.1) -- not expected in
 * practice at the 7-char base62 keyspace (~3.5x10^12).
 */
public class CodeSpaceExhaustedException extends RuntimeException {

    public CodeSpaceExhaustedException(String message) {
        super(message);
    }

    public CodeSpaceExhaustedException(String message, Throwable cause) {
        super(message, cause);
    }
}
