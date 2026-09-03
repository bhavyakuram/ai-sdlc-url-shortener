package com.aisdlc.urlshortener;

import com.aisdlc.urlshortener.data.ShortLinkRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code POST /api/v1/links/batch} batch-scoped rate limiting -- AC36 through AC39
 * (feature-spec.md Section 5, acceptance-criteria.md, R-BULK-1 mitigation).
 *
 * <p>Each test uses its own {@code X-Forwarded-For} source IP (see {@link #freshIp()}), since
 * {@link com.aisdlc.urlshortener.service.BatchRateLimiterService} keys its bucket map on
 * source IP alone -- without a fresh IP per test, cumulative requests across test methods
 * (and across {@link BatchLinkCreationTest}, which shares the same cached Spring context)
 * would flow into the same bucket and make these tests order-dependent. This mirrors {@link
 * RedirectTest}'s own isolation strategy (fresh short codes there; fresh source IPs here,
 * since this limiter has no code to key on before the per-item loop runs).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BatchRateLimitTest {

    private static final AtomicInteger IP_SEQ = new AtomicInteger(100);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ShortLinkRepository shortLinkRepository;

    private static String freshIp() {
        return "10.88.0." + IP_SEQ.getAndIncrement();
    }

    private String oneItemBody(String url) throws Exception {
        return objectMapper.writeValueAsString(Map.of("items", List.of(Map.of("url", url))));
    }

    private String hundredItemBody(String urlPrefix) throws Exception {
        List<Map<String, String>> items = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            items.add(Map.of("url", urlPrefix + i));
        }
        return objectMapper.writeValueAsString(Map.of("items", items));
    }

    /** AC36 -- within the batch rate limit: 200 OK, headers present and decrementing. */
    @Test
    void withinRateLimitReturns200WithDecrementingHeaders() throws Exception {
        String ip = freshIp();
        String body = oneItemBody("https://example.com/rl-within-a");

        mockMvc.perform(post("/api/v1/links/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", ip)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(header().string("X-RateLimit-Limit", "20"))
                .andExpect(header().string("X-RateLimit-Remaining", "19"));

        mockMvc.perform(post("/api/v1/links/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", ip)
                        .content(oneItemBody("https://example.com/rl-within-b")))
                .andExpect(status().isOk())
                .andExpect(header().string("X-RateLimit-Remaining", "18"));
    }

    /**
     * AC37 -- rate limit exceeded: the 21st request in-window is 429, and zero items from
     * that 21st request's body are processed (zero results, zero DB writes), even though the
     * body would otherwise have been entirely valid.
     */
    @Test
    void rateLimitExceededReturns429AndProcessesNothing() throws Exception {
        String ip = freshIp();
        for (int i = 0; i < 20; i++) {
            mockMvc.perform(post("/api/v1/links/batch")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Forwarded-For", ip)
                            .content(oneItemBody("https://example.com/rl-flood-" + i)))
                    .andExpect(status().isOk());
        }

        long countBefore = shortLinkRepository.count();

        mockMvc.perform(post("/api/v1/links/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", ip)
                        .content(oneItemBody("https://example.com/rl-flood-21st")))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"))
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.results").doesNotExist());

        assertThat(shortLinkRepository.count()).isEqualTo(countBefore);
    }

    /**
     * AC38 -- the batch limiter is scoped independently from the redirect limiter: an IP
     * that has exhausted its batch-endpoint limit still redirects successfully via {@code GET
     * /{code}} (a different bucket, different limiter instance entirely).
     */
    @Test
    void batchLimiterIsScopedIndependentlyFromRedirectLimiter() throws Exception {
        String ip = freshIp();
        for (int i = 0; i < 20; i++) {
            mockMvc.perform(post("/api/v1/links/batch")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Forwarded-For", ip)
                            .content(oneItemBody("https://example.com/rl-scope-" + i)))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(post("/api/v1/links/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", ip)
                        .content(oneItemBody("https://example.com/rl-scope-exhausted")))
                .andExpect(status().isTooManyRequests());

        // Create a link (via the single-create endpoint, unaffected by either limiter) then
        // redirect from the same, now batch-exhausted, source IP.
        MvcResult created = mockMvc.perform(post("/api/v1/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("url", "https://example.com/rl-scope-target"))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode json = objectMapper.readTree(created.getResponse().getContentAsString());
        String code = json.get("shortCode").asText();

        mockMvc.perform(get("/" + code).header("X-Forwarded-For", ip))
                .andExpect(status().isFound());
    }

    /**
     * AC39 -- the rate limit is per source IP, not per batch size: 19 requests of 100 items
     * each, then a 20th request of just 1 item still succeeds, and a 21st (1 item) is
     * throttled -- the limiter counts requests, not items.
     */
    @Test
    void rateLimitIsPerRequestNotPerItemCount() throws Exception {
        String ip = freshIp();
        for (int i = 0; i < 19; i++) {
            mockMvc.perform(post("/api/v1/links/batch")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Forwarded-For", ip)
                            .content(hundredItemBody("https://example.com/rl-size-" + i + "-")))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(post("/api/v1/links/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", ip)
                        .content(oneItemBody("https://example.com/rl-size-20th")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/links/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", ip)
                        .content(oneItemBody("https://example.com/rl-size-21st")))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"));
    }
}
