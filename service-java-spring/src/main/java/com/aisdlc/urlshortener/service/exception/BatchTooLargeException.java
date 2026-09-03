package com.aisdlc.urlshortener.service.exception;

/**
 * Thrown by {@link com.aisdlc.urlshortener.service.BulkLinkOrchestrator#processBatch} when
 * {@code items.size() > 100} (feature-spec.md Section 1.1/3.1/4). Maps to
 * {@code 400 Bad Request}, {@code code: BATCH_TOO_LARGE}.
 */
public class BatchTooLargeException extends RuntimeException {

    public BatchTooLargeException(String message) {
        super(message);
    }
}
