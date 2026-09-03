package com.aisdlc.urlshortener.service.exception;

/**
 * A single exception for both "code was never created" and "code exists but is past its
 * {@code expiresAt}" -- feature-spec.md Section 4's deliberate, Gate-0-approved
 * non-disambiguation (a 410 would leak "this code definitely existed once" to an
 * enumeration attempt; nothing downstream needs the distinction). Always maps to
 * {@code 404 Not Found}, {@code code: CODE_NOT_FOUND}, on the redirect path.
 *
 * <p>Deliberately not two separate exception types (e.g. {@code CodeNotFoundException} /
 * {@code LinkExpiredException}) -- see hard constraint 7 in this generator's dispatch and
 * feature-spec.md Section 4 for why collapsing them is correct, not an omission.
 */
public class LinkUnavailableException extends RuntimeException {

    public LinkUnavailableException(String message) {
        super(message);
    }
}
