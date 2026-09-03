package com.aisdlc.urlshortener.service.exception;

/**
 * Thrown by {@link com.aisdlc.urlshortener.service.BulkLinkOrchestrator#processBatch} when
 * {@code items} is {@code null} or empty (feature-spec.md Section 1.1/3.1/4). Maps to
 * {@code 400 Bad Request}, {@code code: EMPTY_BATCH}.
 */
public class EmptyBatchException extends RuntimeException {

    public EmptyBatchException(String message) {
        super(message);
    }
}
