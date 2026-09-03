package com.aisdlc.urlshortener.api.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Shared source-IP extraction for the rate limiter and click-recording geo lookup, so both
 * use the exact same notion of "source IP" for a given request.
 */
public final class ClientIpResolver {

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            // First entry is the original client per the de-facto X-Forwarded-For convention.
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
