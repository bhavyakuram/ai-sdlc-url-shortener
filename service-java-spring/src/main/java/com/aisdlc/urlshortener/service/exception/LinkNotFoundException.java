package com.aisdlc.urlshortener.service.exception;

/** Thrown when a short code was never created (AC-6). */
public class LinkNotFoundException extends RuntimeException {

    public LinkNotFoundException(String code) {
        super("No short link found for code: " + code);
    }
}
