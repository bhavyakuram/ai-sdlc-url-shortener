package com.aisdlc.urlshortener.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * In-process token-bucket rate limiter keyed by source IP only, for {@code POST
 * /api/v1/links/batch} exclusively (feature-spec.md Section 5, R-BULK-1 mitigation). A
 * <b>separate</b> Bucket4j bucket map from {@link RateLimiterService} -- distinct instance,
 * distinct key shape, distinct limit -- not a rescoped or reused instance of the redirect
 * limiter (technical-design.md Section 3).
 *
 * <p>20 requests / rolling 60s window, per source IP -- five times tighter than the redirect
 * limiter's 100/60s, sized against R-BULK-3's evidence that a batch request can drive up to
 * 200 sequential DB round-trips per request rather than the redirect endpoint's single read.
 *
 * <p>Bounded state, same pattern as {@link RateLimiterService}: a synchronized, access-order
 * {@link LinkedHashMap} with {@code removeEldestEntry} gives a bounded LRU eviction policy
 * (feature-spec.md Section 5 storage-bound requirement).
 */
@Service
public class BatchRateLimiterService {

    private static final int LIMIT_PER_MINUTE = 20;
    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final int MAX_TRACKED_KEYS = 50_000;

    private final Map<String, Bucket> buckets = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Bucket> eldest) {
            return size() > MAX_TRACKED_KEYS;
        }
    };

    /**
     * Attempts to consume one token from the bucket for {@code sourceIp}, creating the
     * bucket on first use. One token is consumed per request, not per item -- a request's
     * {@code items.length} has no bearing on how many tokens it consumes (AC39). Always
     * returns a probe (never throws) -- same fail-safe contract as {@link
     * RateLimiterService#tryConsume}.
     */
    public ConsumptionProbe tryConsume(String sourceIp) {
        Bucket bucket;
        synchronized (buckets) {
            bucket = buckets.computeIfAbsent(sourceIp, k -> newBucket());
        }
        return bucket.tryConsumeAndReturnRemaining(1);
    }

    public int limitPerMinute() {
        return LIMIT_PER_MINUTE;
    }

    private static Bucket newBucket() {
        Bandwidth limit = Bandwidth.classic(LIMIT_PER_MINUTE, Refill.intervally(LIMIT_PER_MINUTE, WINDOW));
        return Bucket.builder().addLimit(limit).build();
    }
}
