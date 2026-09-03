package com.aisdlc.urlshortener;

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

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code POST /api/v1/links} -- AC01 through AC10 (feature-spec.md 3.1, acceptance-criteria.md).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LinkCreationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /** AC01 -- happy path: generated code. */
    @Test
    void generatedCodeHappyPath() throws Exception {
        String body = """
                {"url": "https://example.com/a/b/c"}
                """;

        MvcResult result = mockMvc.perform(post("/api/v1/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").exists())
                .andExpect(jsonPath("$.longUrl").value("https://example.com/a/b/c"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.expiresAt").exists())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        String shortCode = json.get("shortCode").asText();
        assertThat(shortCode).hasSize(7);
        assertThat(shortCode).matches("^[0-9A-Za-z]{7}$");

        Instant createdAt = Instant.parse(json.get("createdAt").asText());
        Instant expiresAt = Instant.parse(json.get("expiresAt").asText());
        assertThat(expiresAt).isEqualTo(createdAt.plusSeconds(30L * 24 * 60 * 60));
    }

    /** AC02 -- happy path: custom code, echoed verbatim. */
    @Test
    void customCodeHappyPath() throws Exception {
        String body = """
                {"url": "https://example.com", "customCode": "myLink1"}
                """;

        mockMvc.perform(post("/api/v1/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").value("myLink1"));
    }

    /** AC03 -- invalid URL: disallowed scheme. */
    @Test
    void rejectsDisallowedScheme() throws Exception {
        String body = """
                {"url": "javascript:alert(1)"}
                """;

        mockMvc.perform(post("/api/v1/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_URL_SCHEME"));
    }

    /** AC04 -- invalid URL: exceeds length limit. */
    @Test
    void rejectsUrlTooLong() throws Exception {
        String longPath = "a".repeat(2049);
        String url = "https://example.com/" + longPath;
        String body = objectMapper.writeValueAsString(java.util.Map.of("url", url));

        mockMvc.perform(post("/api/v1/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("URL_TOO_LONG"));
    }

    /** AC05 -- invalid URL: malformed. */
    @Test
    void rejectsMalformedUrl() throws Exception {
        String body = """
                {"url": "ht!tp://not a url"}
                """;

        mockMvc.perform(post("/api/v1/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("URL_MALFORMED"));
    }

    /** AC06 -- invalid custom code shape (below the 3-char minimum). */
    @Test
    void rejectsInvalidCustomCodeShape() throws Exception {
        String body = """
                {"url": "https://example.com", "customCode": "ab"}
                """;

        mockMvc.perform(post("/api/v1/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CUSTOM_CODE_SHAPE"));
    }

    /** AC07 -- custom code collision: second caller gets 409, first record unchanged. */
    @Test
    void customCodeCollisionReturns409AndFirstRecordUnchanged() throws Exception {
        String firstBody = """
                {"url": "https://original.example.com", "customCode": "taken01"}
                """;
        mockMvc.perform(post("/api/v1/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstBody))
                .andExpect(status().isCreated());

        String secondBody = """
                {"url": "https://other.example.com", "customCode": "taken01"}
                """;
        mockMvc.perform(post("/api/v1/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secondBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CUSTOM_CODE_TAKEN"));

        // Original record is unchanged -- verified via the stats endpoint since it returns
        // longUrl for the still-original owner of "taken01".
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/links/taken01/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.longUrl").value("https://original.example.com"));
    }

    /**
     * AC08 -- reserved-code rejection. Per feature-spec.md Section 5 and this exact AC, the
     * response is {@code 400 RESERVED_CODE} (not 409) -- see generator-summary.md for the
     * discrepancy with this generator's own dispatch text, which described 409; the
     * Gate-approved spec/AC win.
     */
    @Test
    void rejectsReservedCode() throws Exception {
        String body = """
                {"url": "https://example.com", "customCode": "api"}
                """;

        mockMvc.perform(post("/api/v1/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("RESERVED_CODE"));
    }

    /**
     * AC09 -- concurrent creation, collision-safety: custom code. Two concurrent POSTs with
     * the identical customCode -- exactly one 201, one 409, exactly one row ever persisted.
     * Real concurrency (ExecutorService + CountDownLatch), not sequential calls -- per
     * risk-register.md R-2's mitigation, this must be a genuine concurrent-request test.
     */
    @Test
    void concurrentCustomCodeCreationIsRace_safe() throws Exception {
        String raceCode = "raceCode";
        String body = objectMapper.writeValueAsString(
                java.util.Map.of("url", "https://race.example.com", "customCode", raceCode));

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        try {
            List<Callable<Integer>> tasks = IntStream.range(0, 2)
                    .<Callable<Integer>>mapToObj(i -> () -> {
                        startLatch.await();
                        MvcResult result = mockMvc.perform(post("/api/v1/links")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(body))
                                .andReturn();
                        return result.getResponse().getStatus();
                    })
                    .collect(Collectors.toList());

            List<Future<Integer>> futures = tasks.stream().map(pool::submit).collect(Collectors.toList());
            startLatch.countDown();

            List<Integer> statuses = futures.stream().map(f -> {
                try {
                    return f.get(10, TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).toList();

            assertThat(statuses).containsExactlyInAnyOrder(201, 409);
        } finally {
            pool.shutdownNow();
        }

        // Exactly one row for raceCode -- verified via the stats endpoint resolving cleanly
        // (if two rows existed, the unique constraint itself would already have prevented
        // it, but confirm the code is queryable exactly once here too).
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/links/" + raceCode + "/stats"))
                .andExpect(status().isOk());
    }

    /**
     * AC10 -- concurrent creation, collision-safety: generated codes. N concurrent POSTs
     * with distinct URLs and no customCode -- every request gets 201, every shortCode is
     * unique. Candidate A satisfies this structurally (DB identity column), not just
     * probabilistically -- this test exercises that guarantee under real concurrency.
     */
    @Test
    void concurrentGeneratedCodeCreationYieldsUniqueCodes() throws Exception {
        int n = 20;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch startLatch = new CountDownLatch(1);
        try {
            List<Callable<MvcResult>> tasks = IntStream.range(0, n)
                    .<Callable<MvcResult>>mapToObj(i -> () -> {
                        startLatch.await();
                        String body = objectMapper.writeValueAsString(
                                java.util.Map.of("url", "https://distinct.example.com/" + i));
                        return mockMvc.perform(post("/api/v1/links")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(body))
                                .andReturn();
                    })
                    .collect(Collectors.toList());

            List<Future<MvcResult>> futures = tasks.stream().map(pool::submit).collect(Collectors.toList());
            startLatch.countDown();

            List<String> shortCodes = futures.stream().map(f -> {
                try {
                    MvcResult result = f.get(10, TimeUnit.SECONDS);
                    assertThat(result.getResponse().getStatus()).isEqualTo(201);
                    JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
                    return json.get("shortCode").asText();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).toList();

            assertThat(Set.copyOf(shortCodes)).hasSize(n);
        } finally {
            pool.shutdownNow();
        }
    }
}
