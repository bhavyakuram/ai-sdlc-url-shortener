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
 * {@code POST /api/v1/links/batch} -- AC26 through AC32, AC34, AC35, and AC40 (feature-spec.md
 * 3.1, acceptance-criteria.md). AC33 (CODE_SPACE_EXHAUSTED) is covered separately in {@link
 * BatchCodeSpaceExhaustedTest} (requires mocking {@code LinkService} to force the pathological
 * path); AC36-AC39 (batch-scoped rate limiting) are covered separately in {@link
 * BatchRateLimitTest}.
 *
 * <p>Every test that hits the batch endpoint uses its own {@code X-Forwarded-For} source IP
 * (see {@link #freshIp()}) so this class's requests never share a {@code
 * BatchRateLimiterService} bucket (keyed on source IP only) with each other or with {@link
 * BatchRateLimitTest} -- the same isolation principle {@link RedirectTest} applies via fresh
 * short codes for its own per-(IP, code) limiter.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BatchLinkCreationTest {

    private static final AtomicInteger IP_SEQ = new AtomicInteger(1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ShortLinkRepository shortLinkRepository;

    private static String freshIp() {
        return "10.77.0." + IP_SEQ.getAndIncrement();
    }

    /** AC26 -- happy path: all items succeed. */
    @Test
    void allItemsSucceed() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("items", List.of(
                Map.of("url", "https://example.com/batch-a"),
                Map.of("url", "https://example.com/batch-b"),
                Map.of("url", "https://example.com/batch-c"))));

        MvcResult result = mockMvc.perform(post("/api/v1/links/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", freshIp())
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results.length()").value(3))
                .andExpect(jsonPath("$.results[0].status").value("CREATED"))
                .andExpect(jsonPath("$.results[1].status").value("CREATED"))
                .andExpect(jsonPath("$.results[2].status").value("CREATED"))
                .andExpect(jsonPath("$.successCount").value(3))
                .andExpect(jsonPath("$.failureCount").value(0))
                .andReturn();

        JsonNode first = objectMapper.readTree(result.getResponse().getContentAsString()).get("results").get(0);
        assertThat(first.get("shortCode").asText()).isNotBlank();
        assertThat(first.get("shortUrl").asText()).isNotBlank();
        assertThat(first.get("longUrl").asText()).isEqualTo("https://example.com/batch-a");
        assertThat(first.has("createdAt")).isTrue();
        assertThat(first.has("expiresAt")).isTrue();
        // @JsonInclude(NON_NULL) -- a CREATED entry must not carry FAILED-only fields.
        assertThat(first.has("code")).isFalse();
        assertThat(first.has("message")).isFalse();
    }

    /** AC27 -- mixed outcome: partial success and multiple distinct failure reasons. */
    @Test
    void mixedOutcomePartialSuccessAndDistinctFailures() throws Exception {
        // Pre-create a link owning "alias1" so item C's customCode collides with an
        // already-persisted link (feature-spec.md 3.1's own example scenario).
        mockMvc.perform(post("/api/v1/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("url", "https://example.com/original-alias1", "customCode", "alias1"))))
                .andExpect(status().isCreated());

        String body = objectMapper.writeValueAsString(Map.of("items", List.of(
                Map.of("url", "https://example.com/batch-mixed-a"),
                Map.of("url", "ht!tp://not a url"),
                Map.of("url", "https://example.com/batch-mixed-c", "customCode", "alias1"))));

        mockMvc.perform(post("/api/v1/links/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", freshIp())
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].status").value("CREATED"))
                .andExpect(jsonPath("$.results[1].status").value("FAILED"))
                .andExpect(jsonPath("$.results[1].code").value("URL_MALFORMED"))
                .andExpect(jsonPath("$.results[2].status").value("FAILED"))
                .andExpect(jsonPath("$.results[2].code").value("CUSTOM_CODE_TAKEN"))
                .andExpect(jsonPath("$.successCount").value(1))
                .andExpect(jsonPath("$.failureCount").value(2));
    }

    /** AC28 -- empty batch rejected at the whole-request level, zero DB writes. */
    @Test
    void emptyBatchRejectedAtWholeRequestLevel() throws Exception {
        long countBefore = shortLinkRepository.count();

        mockMvc.perform(post("/api/v1/links/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", freshIp())
                        .content("{\"items\": []}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMPTY_BATCH"))
                .andExpect(jsonPath("$.results").doesNotExist());

        assertThat(shortLinkRepository.count()).isEqualTo(countBefore);
    }

    /** AC29 -- over-limit batch (101 items) rejected at the whole-request level, zero DB writes. */
    @Test
    void overLimitBatchRejectedAtWholeRequestLevel() throws Exception {
        long countBefore = shortLinkRepository.count();
        List<Map<String, String>> items = new ArrayList<>();
        for (int i = 0; i < 101; i++) {
            items.add(Map.of("url", "https://example.com/over-limit-" + i));
        }
        String body = objectMapper.writeValueAsString(Map.of("items", items));

        mockMvc.perform(post("/api/v1/links/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", freshIp())
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BATCH_TOO_LARGE"));

        assertThat(shortLinkRepository.count()).isEqualTo(countBefore);
    }

    /** AC30 -- boundary: exactly 100 items is accepted, not rejected. */
    @Test
    void exactlyOneHundredItemsIsAccepted() throws Exception {
        List<Map<String, String>> items = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            items.add(Map.of("url", "https://example.com/boundary-100-" + i));
        }
        String body = objectMapper.writeValueAsString(Map.of("items", items));

        mockMvc.perform(post("/api/v1/links/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", freshIp())
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results.length()").value(100))
                .andExpect(jsonPath("$.successCount").value(100))
                .andExpect(jsonPath("$.failureCount").value(0));
    }

    /**
     * AC31 -- whole-request validation runs before any per-item work: 150 items (first 100
     * individually valid) still produces zero writes, because the batch-size check runs
     * before the per-item loop starts.
     */
    @Test
    void wholeRequestValidationRunsBeforeAnyPerItemWork() throws Exception {
        long countBefore = shortLinkRepository.count();
        List<Map<String, String>> items = new ArrayList<>();
        for (int i = 0; i < 150; i++) {
            items.add(Map.of("url", "https://example.com/zero-writes-" + i));
        }
        String body = objectMapper.writeValueAsString(Map.of("items", items));

        mockMvc.perform(post("/api/v1/links/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", freshIp())
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BATCH_TOO_LARGE"));

        assertThat(shortLinkRepository.count()).isEqualTo(countBefore);
    }

    /** AC32 -- response order matches input order 1:1, regardless of outcome interleaving. */
    @Test
    void responseOrderMatchesInputOrder() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("items", List.of(
                Map.of("url", "ht!tp://not a url"),
                Map.of("url", "https://example.com/order-b"),
                Map.of("url", "ht!tp://not a url"),
                Map.of("url", "https://example.com/order-d"))));

        mockMvc.perform(post("/api/v1/links/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", freshIp())
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].status").value("FAILED"))
                .andExpect(jsonPath("$.results[1].status").value("CREATED"))
                .andExpect(jsonPath("$.results[1].longUrl").value("https://example.com/order-b"))
                .andExpect(jsonPath("$.results[2].status").value("FAILED"))
                .andExpect(jsonPath("$.results[3].status").value("CREATED"))
                .andExpect(jsonPath("$.results[3].longUrl").value("https://example.com/order-d"));
    }

    /**
     * AC34 -- a failing item does not affect any other item's persistence: exactly 2 rows
     * persisted for [succeed-A, fail-B, succeed-C], and both A and C are independently
     * queryable via the stats endpoint immediately after the batch response returns.
     */
    @Test
    void failingItemDoesNotAffectOtherItemsPersistence() throws Exception {
        long countBefore = shortLinkRepository.count();
        String body = objectMapper.writeValueAsString(Map.of("items", List.of(
                Map.of("url", "https://example.com/iso-a"),
                Map.of("url", "ht!tp://not a url"),
                Map.of("url", "https://example.com/iso-c"))));

        MvcResult result = mockMvc.perform(post("/api/v1/links/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", freshIp())
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successCount").value(2))
                .andExpect(jsonPath("$.failureCount").value(1))
                .andReturn();

        assertThat(shortLinkRepository.count()).isEqualTo(countBefore + 2);

        JsonNode results = objectMapper.readTree(result.getResponse().getContentAsString()).get("results");
        String codeA = results.get(0).get("shortCode").asText();
        String codeC = results.get(2).get("shortCode").asText();

        mockMvc.perform(get("/api/v1/links/" + codeA + "/stats")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/links/" + codeC + "/stats")).andExpect(status().isOk());
    }

    /**
     * AC35 -- a later item's failure does not roll back an earlier item's success: item A's
     * commit is durable and immediately redirectable even though item B (later in the same
     * request) fails on a custom-code collision.
     */
    @Test
    void laterItemFailureDoesNotRollBackEarlierItemSuccess() throws Exception {
        // Pre-create a link owning "collideMe" so item B's customCode collides.
        mockMvc.perform(post("/api/v1/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("url", "https://example.com/pre-existing", "customCode", "collideMe"))))
                .andExpect(status().isCreated());

        String body = objectMapper.writeValueAsString(Map.of("items", List.of(
                Map.of("url", "https://example.com/iso-order-a"),
                Map.of("url", "https://example.com/iso-order-b", "customCode", "collideMe"))));

        MvcResult result = mockMvc.perform(post("/api/v1/links/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", freshIp())
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].status").value("CREATED"))
                .andExpect(jsonPath("$.results[1].status").value("FAILED"))
                .andExpect(jsonPath("$.results[1].code").value("CUSTOM_CODE_TAKEN"))
                .andReturn();

        String codeA = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("results").get(0).get("shortCode").asText();

        mockMvc.perform(get("/" + codeA))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/iso-order-a"));
    }

    /**
     * AC40 -- {@code POST /api/v1/links} behavior is unchanged by this feature: same 201
     * body shape, and no batch-limiter headers leak onto the single-create endpoint (it isn't
     * registered against {@code /api/v1/links/batch}, only the redirect limiter's unrelated,
     * pre-existing {@code /api/**} exclusion applies here, unchanged).
     */
    @Test
    void singleCreateEndpointBehaviorIsUnchanged() throws Exception {
        String body = """
                {"url": "https://example.com/regression-check"}
                """;

        mockMvc.perform(post("/api/v1/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").exists())
                .andExpect(jsonPath("$.longUrl").value("https://example.com/regression-check"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.expiresAt").exists())
                .andExpect(header().doesNotExist("X-RateLimit-Limit"))
                .andExpect(header().doesNotExist("X-RateLimit-Remaining"));
    }
}
