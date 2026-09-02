package com.aisdlc.urlshortener.service.exception;

/**
 * Thrown when a short code existed but has expired (AC-7). Deliberately
 * distinct from {@link LinkNotFoundException} so callers can tell
 * "never existed" (404) from "existed, now gone" (410) — see
 * step2/ux-flow.md Interaction Rules.
 */
public class LinkExpiredException extends RuntimeException {

    public LinkExpiredException(String code) {
        super("Short link expired: " + code);
    }
}
