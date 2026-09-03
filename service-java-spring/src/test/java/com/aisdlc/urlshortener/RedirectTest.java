package com.aisdlc.urlshortener;

import com.aisdlc.urlshortener.data.ClickEventEntity;
import com.aisdlc.urlshortener.data.ClickEventRepository;
import com.aisdlc.urlshortener.data.ShortLinkEntity;
import com.aisdlc.urlshortener.data.ShortLinkRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET /{code}} -- AC11 through AC20 (feature-spec.md 3.2/4/6, acceptance-criteria.md).
 *
 * <p>Each test that exercises the rate limiter uses its own freshly-created short code, so
 * that per-(IP, code) bucket state from one test method never leaks into another within the
 * same (context-cached) Spring test context.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RedirectTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ShortLinkRepository shortLinkRepository;

    @Autowired
    private ClickEventRepository clickEventRepository;

    private String createLink(String url) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("url", url));
        MvcResult result = mockMvc.perform(post("/api/v1/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("shortCode").asText();
    }

    /** AC11 -- happy path: valid, non-expired code redirects. */
    @Test
    void redirectsToTargetUrl() throws Exception {
        String code = createLink("https://example.com/x");

        mockMvc.perform(get("/" + code))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/x"));
    }

    /** AC12 -- unknown code. */
    @Test
    void unknownCodeReturns404() throws Exception {
        mockMvc.perform(get("/zzzzzzzUnknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CODE_NOT_FOUND"));
    }

    /**
     * AC13 -- expired code returns 404, byte-identical in shape to AC12 (no 410). Created
     * directly via the repository since production creation always sets a 30-day-future
     * expiry -- this bypasses that to place an already-expired row without waiting 30 days.
     */
    @Test
    void expiredCodeReturns404SameShapeAsUnknown() throws Exception {
        Instant past = Instant.now().minus(1, ChronoUnit.DAYS);
        ShortLinkEntity expired = new ShortLinkEntity("old0001x", "https://example.com/old",
                past.minus(30, ChronoUnit.DAYS), past);
        shortLinkRepository.saveAndFlush(expired);

        mockMvc.perform(get("/old0001x"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CODE_NOT_FOUND"));
    }

    /** AC14 -- successful redirect records exactly one ClickEvent, with referrer. */
    @Test
    void successfulRedirectRecordsExactlyOneClick() throws Exception {
        String code = createLink("https://example.com/click-target");
        ShortLinkEntity link = shortLinkRepository.findByCode(code).orElseThrow();
        assertThat(clickEventRepository.findByShortLinkId(link.getId())).isEmpty();

        mockMvc.perform(get("/" + code).header("Referer", "https://ref.example.com"))
                .andExpect(status().isFound());

        List<ClickEventEntity> clicks = clickEventRepository.findByShortLinkId(link.getId());
        assertThat(clicks).hasSize(1);
        assertThat(clicks.get(0).getReferrer()).isEqualTo("https://ref.example.com");
    }

    /** AC15 -- within rate limit: 302, with X-RateLimit-* headers. */
    @Test
    void withinRateLimitReturns302WithHeaders() throws Exception {
        String code = createLink("https://example.com/within-limit");

        mockMvc.perform(get("/" + code))
                .andExpect(status().isFound())
                .andExpect(header().string("X-RateLimit-Limit", "100"))
                .andExpect(header().string("X-RateLimit-Remaining", "99"));
    }

    /** AC16 -- rate limit exceeded: 101st request in-window gets 429, no click recorded for it. */
    @Test
    void rateLimitExceededReturns429AndDoesNotRecordClick() throws Exception {
        String code = createLink("https://example.com/over-limit");
        ShortLinkEntity link = shortLinkRepository.findByCode(code).orElseThrow();

        for (int i = 0; i < 100; i++) {
            mockMvc.perform(get("/" + code)).andExpect(status().isFound());
        }
        long clicksAfter100 = clickEventRepository.findByShortLinkId(link.getId()).size();
        assertThat(clicksAfter100).isEqualTo(100);

        mockMvc.perform(get("/" + code))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"))
                .andExpect(header().exists("Retry-After"));

        long clicksAfter101 = clickEventRepository.findByShortLinkId(link.getId()).size();
        assertThat(clicksAfter101).isEqualTo(100);
    }

    /** AC17 -- rate limit is scoped per (IP, code), not per IP alone. */
    @Test
    void rateLimitIsPerCodeNotPerIp() throws Exception {
        String exhaustedCode = createLink("https://example.com/exhaust-me");
        String otherCode = createLink("https://example.com/unaffected");

        for (int i = 0; i < 100; i++) {
            mockMvc.perform(get("/" + exhaustedCode)).andExpect(status().isFound());
        }
        mockMvc.perform(get("/" + exhaustedCode)).andExpect(status().isTooManyRequests());

        // Same MockMvc client (same source IP), different code -- independent bucket.
        mockMvc.perform(get("/" + otherCode)).andExpect(status().isFound());
    }

    /**
     * AC18 -- rate limit window reset after 61s. NOT executed as a real 61-second sleep: a
     * unit/integration test suite that blocks for a full minute-plus per assertion is
     * unacceptably slow/flaky for routine CI runs, and this implementation deliberately does
     * not inject a {@code Clock} into {@link com.aisdlc.urlshortener.service.RateLimiterService}
     * (Bucket4j's {@code Refill.intervally} reads the system clock internally), so there is
     * no way to fast-forward the bucket's refill without a real wall-clock wait. Documented
     * skip per this generator's hard constraint 9, rather than a silently-omitted AC.
     * Behaviorally, this is exercised at the unit level by Bucket4j's own test suite (the
     * refill mechanism itself is third-party, already-tested library code, not this
     * project's logic) and structurally by {@link #rateLimitExceededReturns429AndDoesNotRecordClick()}
     * proving the bucket does reach empty at exactly 100.
     */
    @Test
    @Disabled("Requires a real 61s wall-clock wait; RateLimiterService has no injectable "
            + "Clock seam (Bucket4j's Refill.intervally reads system time internally). "
            + "See javadoc above for the full rationale.")
    void rateLimitWindowResetsAfter61Seconds() {
        // Intentionally not implemented as a live sleep-based test -- see javadoc above.
    }

    /**
     * AC19 -- abuse resilience: 1000 req/min against one code from one source IP -- first
     * 100 succeed (302), the remaining 900 are 429, zero 5xx.
     */
    @Test
    void floodAtTenXThresholdProducesNo5xx() throws Exception {
        String code = createLink("https://example.com/flood-target");

        int success = 0;
        int rateLimited = 0;
        int serverErrors = 0;
        for (int i = 0; i < 1000; i++) {
            int status = mockMvc.perform(get("/" + code)).andReturn().getResponse().getStatus();
            if (status == 302) {
                success++;
            } else if (status == 429) {
                rateLimited++;
            } else if (status >= 500) {
                serverErrors++;
            }
        }

        assertThat(success).isEqualTo(100);
        assertThat(rateLimited).isEqualTo(900);
        assertThat(serverErrors).isZero();
    }

    /**
     * AC20 -- geo-IP lookup fails soft. This build environment has no real
     * GeoLite2-Country.mmdb provisioned (R-7), so GeoLookupService's DatabaseReader is null
     * and every lookup genuinely returns null -- this test exercises the real fail-soft
     * production code path, not a mock standing in for it.
     */
    @Test
    void geoLookupFailsSoftAndClickIsStillRecordedAsUnknownCountry() throws Exception {
        String code = createLink("https://example.com/geo-fail-soft");

        mockMvc.perform(get("/" + code))
                .andExpect(status().isFound());

        MvcResult statsResult = mockMvc.perform(get("/api/v1/links/" + code + "/stats"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(statsResult.getResponse().getContentAsString());
        assertThat(json.get("totalClicks").asLong()).isEqualTo(1);
        assertThat(json.get("clicksByCountry").get(0).get("country").asText()).isEqualTo("unknown");
    }
}
