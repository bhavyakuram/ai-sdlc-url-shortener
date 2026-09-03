package com.aisdlc.urlshortener.service.exception;

/**
 * {@code customCode} was supplied but is outside the 3-32 char / base62 shape required by
 * FR-8. Maps to {@code 400 Bad Request}, {@code code: INVALID_CUSTOM_CODE_SHAPE}.
 */
public class InvalidCustomCodeShapeException extends RuntimeException {

    public InvalidCustomCodeShapeException(String message) {
        super(message);
    }
}
