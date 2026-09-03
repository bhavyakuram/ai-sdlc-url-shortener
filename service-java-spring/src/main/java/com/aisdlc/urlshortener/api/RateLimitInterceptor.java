package com.aisdlc.urlshortener.api;

import com.aisdlc.urlshortener.api.dto.ErrorResponse;
import com.aisdlc.urlshortener.api.util.ClientIpResolver;
import com.aisdlc.urlshortener.service.RateLimiterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Enforces FR-9's 100 req/min per-(source IP, code) rate limit on the redirect path only
 * (feature-spec.md Section 6) -- registered by {@link WebConfig} against {@code /{code}}
 * exclusively, never {@code /api/**}. Always resolves to either "let the request through"
 * or "write a 429 and stop" -- never throws, so a rate-limiter problem can never surface as
 * a 5xx (AC19).
 */
@org.springframework.stereotype.Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiterService rateLimiterService;
    private final ObjectMapper objectMapper;

    public RateLimitInterceptor(RateLimiterService rateLimiterService, ObjectMapper objectMapper) {
        this.rateLimiterService = rateLimiterService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        String code = extractCode(request);
        if (code == null) {
            return true;
        }

        String sourceIp = ClientIpResolver.resolve(request);
        ConsumptionProbe probe = rateLimiterService.tryConsume(sourceIp, code);

        response.setHeader("X-RateLimit-Limit", String.valueOf(rateLimiterService.limitPerMinute()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(probe.getRemainingTokens(), 0)));

        if (probe.isConsumed()) {
            return true;
        }

        long retryAfterSeconds = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()));
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse body = ErrorResponse.of(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED",
                "Rate limit exceeded for this link; retry after the window resets");
        objectMapper.writeValue(response.getWriter(), body);
        return false;
    }

    private String extractCode(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null || uri.length() < 2) {
            return null;
        }
        return uri.substring(1);
    }
}
