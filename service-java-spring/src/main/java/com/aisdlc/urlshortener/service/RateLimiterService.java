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
 * In-process token-bucket rate limiter keyed by {@code (source IP, short code)}, per
 * feature-spec.md Section 6 / FR-9. 100 requests / rolling 60s window.
 *
 * <p>Bucket4j (per feasibility-report.md Part 2 friction point #2) was picked over a
 * hand-rolled bucket specifically because its operations are documented thread-safe/atomic
 * -- risk-register.md R-4's mitigation.
 *
 * <p><b>Bounded state (risk-register.md R-8):</b> FR-6 means there are no accounts and no
 * cap on distinct (IP, code) pairs, so an unbounded map would let a flood of distinct pairs
 * grow this service's memory without limit. A synchronized, access-order {@link
 * LinkedHashMap} with {@code removeEldestEntry} gives a bounded LRU eviction policy without
 * introducing a new caching dependency beyond the ones step1's feasibility-report.md
 * approved (Bucket4j only).
 */
@Service
public class RateLimiterService {

    private static final int LIMIT_PER_MINUTE = 100;
    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final int MAX_TRACKED_KEYS = 50_000;

    private final Map<String, Bucket> buckets = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Bucket> eldest) {
            return size() > MAX_TRACKED_KEYS;
        }
    };

    /**
     * Attempts to consume one token from the bucket for {@code (sourceIp, code)}, creating
     * the bucket on first use. Always returns a probe (never throws) -- a rate limiter that
     * can fail closed with a 5xx would defeat its own abuse-resilience purpose (AC19).
     */
    public ConsumptionProbe tryConsume(String sourceIp, String code) {
        String key = sourceIp + '|' + code;
        Bucket bucket;
        synchronized (buckets) {
            bucket = buckets.computeIfAbsent(key, k -> newBucket());
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
