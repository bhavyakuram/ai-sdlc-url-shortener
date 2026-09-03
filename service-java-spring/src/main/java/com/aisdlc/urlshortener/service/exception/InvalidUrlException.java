package com.aisdlc.urlshortener.service.exception;

/**
 * The submitted {@code url} failed one of FR-3's validation rules. Carries the exact
 * error-code vocabulary token from {@code feature-spec.md} Section 1.1 so the api layer
 * doesn't have to re-derive which rule failed -- always maps to {@code 400 Bad Request}.
 */
public class InvalidUrlException extends RuntimeException {

    /** One of INVALID_URL_SCHEME | URL_TOO_LONG | URL_MALFORMED. */
    private final String errorCode;

    public InvalidUrlException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
