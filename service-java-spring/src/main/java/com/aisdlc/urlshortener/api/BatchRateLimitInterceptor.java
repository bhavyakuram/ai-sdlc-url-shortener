package com.aisdlc.urlshortener.api;

import com.aisdlc.urlshortener.api.dto.ErrorResponse;
import com.aisdlc.urlshortener.api.util.ClientIpResolver;
import com.aisdlc.urlshortener.service.BatchRateLimiterService;
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
 * Enforces the 20 req/60s per-source-IP rate limit on {@code POST /api/v1/links/batch} only
 * (feature-spec.md Section 5, R-BULK-1 mitigation) -- registered by {@link WebConfig} against
 * {@code "/api/v1/links/batch"} exclusively via a second, separate interceptor registration.
 * Does <b>not</b> reuse, reopen, or affect {@link RateLimitInterceptor}'s {@code (IP, code)}
 * bucket for the redirect path (AC38) -- a structurally distinct bean backed by a distinct
 * {@link BatchRateLimiterService} bucket map.
 *
 * <p>Runs in {@code preHandle}, before Spring MVC deserializes the request body, so a
 * throttled request never pays a JSON-parse or per-item-loop cost (feature-spec.md Section 4
 * ordering: rate-limit -&gt; parse -&gt; batch-size -&gt; per-item loop). Never throws --
 * always either lets the request through or writes a 429 and stops, the same fail-safe
 * contract {@link RateLimitInterceptor} follows.
 */
@org.springframework.stereotype.Component
public class BatchRateLimitInterceptor implements HandlerInterceptor {

    private final BatchRateLimiterService batchRateLimiterService;
    private final ObjectMapper objectMapper;

    public BatchRateLimitInterceptor(BatchRateLimiterService batchRateLimiterService, ObjectMapper objectMapper) {
        this.batchRateLimiterService = batchRateLimiterService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        String sourceIp = ClientIpResolver.resolve(request);
        ConsumptionProbe probe = batchRateLimiterService.tryConsume(sourceIp);

        response.setHeader("X-RateLimit-Limit", String.valueOf(batchRateLimiterService.limitPerMinute()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(probe.getRemainingTokens(), 0)));

        if (probe.isConsumed()) {
            return true;
        }

        long retryAfterSeconds = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()));
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse body = ErrorResponse.of(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED",
                "Rate limit exceeded for batch link creation; retry after the window resets");
        objectMapper.writeValue(response.getWriter(), body);
        return false;
    }
}
