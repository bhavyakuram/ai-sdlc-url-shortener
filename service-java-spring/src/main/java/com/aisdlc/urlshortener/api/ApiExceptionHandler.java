package com.aisdlc.urlshortener.api;

import com.aisdlc.urlshortener.api.dto.ErrorResponse;
import com.aisdlc.urlshortener.service.exception.AliasTakenException;
import com.aisdlc.urlshortener.service.exception.LinkExpiredException;
import com.aisdlc.urlshortener.service.exception.LinkNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Central mapping from service-layer exceptions to the HTTP responses
 * defined in step3/api-contract.yaml. Every branch here corresponds to
 * a specific AC in step2/acceptance-criteria.md.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    // AC-4: invalid targetUrl / invalid alias shape -> 400
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .orElse("Invalid request");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_REQUEST", message));
    }

    // AC-3: alias collision -> 409, machine-readable code=ALIAS_TAKEN
    @ExceptionHandler(AliasTakenException.class)
    public ResponseEntity<ErrorResponse> handleAliasTaken(AliasTakenException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("ALIAS_TAKEN", ex.getMessage()));
    }

    // AC-6: unknown code -> 404
    @ExceptionHandler(LinkNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(LinkNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("LINK_NOT_FOUND", ex.getMessage()));
    }

    // AC-7: expired code -> 410, distinct from 404 (step2/ux-flow.md)
    @ExceptionHandler(LinkExpiredException.class)
    public ResponseEntity<ErrorResponse> handleExpired(LinkExpiredException ex) {
        return ResponseEntity.status(HttpStatus.GONE)
                .body(new ErrorResponse("LINK_EXPIRED", ex.getMessage()));
    }
}
