package com.aisdlc.urlshortener.api;

import com.aisdlc.urlshortener.api.dto.ErrorResponse;
import com.aisdlc.urlshortener.service.exception.BatchTooLargeException;
import com.aisdlc.urlshortener.service.exception.CodeSpaceExhaustedException;
import com.aisdlc.urlshortener.service.exception.CustomCodeTakenException;
import com.aisdlc.urlshortener.service.exception.EmptyBatchException;
import com.aisdlc.urlshortener.service.exception.InvalidCustomCodeShapeException;
import com.aisdlc.urlshortener.service.exception.InvalidUrlException;
import com.aisdlc.urlshortener.service.exception.LinkUnavailableException;
import com.aisdlc.urlshortener.service.exception.ReservedCodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps every service-layer exception to the error envelope + status/{@code code} vocabulary
 * defined in feature-spec.md Section 1.1. Lives in the api layer, not service -- service
 * exceptions carry no HTTP concerns of their own (rules/architecture.md Dependency
 * Direction).
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(InvalidUrlException.class)
    public ResponseEntity<ErrorResponse> handleInvalidUrl(InvalidUrlException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getErrorCode(), ex.getMessage());
    }

    @ExceptionHandler(InvalidCustomCodeShapeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCustomCodeShape(InvalidCustomCodeShapeException ex) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_CUSTOM_CODE_SHAPE", ex.getMessage());
    }

    @ExceptionHandler(ReservedCodeException.class)
    public ResponseEntity<ErrorResponse> handleReservedCode(ReservedCodeException ex) {
        // 400, not 409 -- feature-spec.md Section 5 and AC08 both pin this exact status;
        // see generator-summary.md for the discrepancy with this dispatch's own hard
        // constraint 8 text, resolved in favor of the Gate-approved spec/AC.
        return build(HttpStatus.BAD_REQUEST, "RESERVED_CODE", ex.getMessage());
    }

    @ExceptionHandler(CustomCodeTakenException.class)
    public ResponseEntity<ErrorResponse> handleCustomCodeTaken(CustomCodeTakenException ex) {
        return build(HttpStatus.CONFLICT, "CUSTOM_CODE_TAKEN", ex.getMessage());
    }

    @ExceptionHandler(LinkUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleLinkUnavailable(LinkUnavailableException ex) {
        return build(HttpStatus.NOT_FOUND, "CODE_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(CodeSpaceExhaustedException.class)
    public ResponseEntity<ErrorResponse> handleCodeSpaceExhausted(CodeSpaceExhaustedException ex) {
        log.error("Code space exhausted while creating a short link", ex);
        return build(HttpStatus.SERVICE_UNAVAILABLE, "CODE_SPACE_EXHAUSTED", ex.getMessage());
    }

    /** feature-spec.md Section 1.1 -- {@code items} missing, {@code null}, or {@code []}. */
    @ExceptionHandler(EmptyBatchException.class)
    public ResponseEntity<ErrorResponse> handleEmptyBatch(EmptyBatchException ex) {
        return build(HttpStatus.BAD_REQUEST, "EMPTY_BATCH", ex.getMessage());
    }

    /** feature-spec.md Section 1.1 -- {@code items.length > 100}. */
    @ExceptionHandler(BatchTooLargeException.class)
    public ResponseEntity<ErrorResponse> handleBatchTooLarge(BatchTooLargeException ex) {
        return build(HttpStatus.BAD_REQUEST, "BATCH_TOO_LARGE", ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Request body is missing or malformed");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unhandled exception processing request", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred");
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(ErrorResponse.of(status, code, message));
    }
}
