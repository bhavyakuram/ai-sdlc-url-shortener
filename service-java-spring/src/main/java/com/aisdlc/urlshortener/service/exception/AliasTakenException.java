package com.aisdlc.urlshortener.service.exception;

/** Thrown when a caller-requested alias (AC-3) is already in use. */
public class AliasTakenException extends RuntimeException {

    public AliasTakenException(String alias) {
        super("Alias already in use: " + alias);
    }
}
