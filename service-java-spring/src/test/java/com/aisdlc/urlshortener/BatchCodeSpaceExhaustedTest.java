package com.aisdlc.urlshortener;

import com.aisdlc.urlshortener.data.ShortLinkEntity;
import com.aisdlc.urlshortener.service.LinkService;
import com.aisdlc.urlshortener.service.exception.CodeSpaceExhaustedException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AC33 -- {@code CODE_SPACE_EXHAUSTED} surfaces per-item, never as a whole-batch failure.
 *
 * <p>This pathological case (the generated-code retry budget exhausted for one specific item)
 * cannot be forced deterministically through the real {@code CodeGenerator}/H2 stack via
 * black-box HTTP calls: it requires a {@link org.springframework.dao.DataIntegrityViolationException}
 * on a specific {@code saveAndFlush} call inside {@link LinkService#createLink}, and
 * technical-design.md itself describes this path as "vanishingly unlikely... at the 7-char
 * base62 keyspace (~3.5x10^12)" by construction -- there is no seam to force it deterministically
 * short of forcing a real hash collision. Verified instead with a Spring Boot Test that mocks
 * {@link LinkService} (the one genuinely-unforceable dependency) to make it throw {@link
 * CodeSpaceExhaustedException} for exactly one item while a sibling item succeeds normally --
 * this still exercises the real HTTP endpoint, {@code BulkLinkOrchestrator}'s real per-item
 * catch/map logic (the actual behavior this AC verifies), and the real controller response
 * assembly; only the exception-triggering condition itself is substituted.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BatchCodeSpaceExhaustedTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LinkService linkService;

    /** AC33 -- item A's pathological CODE_SPACE_EXHAUSTED failure does not affect item B. */
    @Test
    void codeSpaceExhaustedIsPerItemNotWholeBatch() throws Exception {
        Instant now = Instant.now();
        ShortLinkEntity linkB = new ShortLinkEntity("gen1234", "https://example.com/b", now,
                now.plus(30, ChronoUnit.DAYS));

        when(linkService.createLink(eq("https://example.com/a"), isNull()))
                .thenThrow(new CodeSpaceExhaustedException(
                        "Short code space collision deriving a code for this request; retry"));
        when(linkService.createLink(eq("https://example.com/b"), isNull()))
                .thenReturn(linkB);

        String body = objectMapper.writeValueAsString(Map.of("items", List.of(
                Map.of("url", "https://example.com/a"),
                Map.of("url", "https://example.com/b"))));

        mockMvc.perform(post("/api/v1/links/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].status").value("FAILED"))
                .andExpect(jsonPath("$.results[0].code").value("CODE_SPACE_EXHAUSTED"))
                .andExpect(jsonPath("$.results[1].status").value("CREATED"))
                .andExpect(jsonPath("$.results[1].shortCode").value("gen1234"))
                .andExpect(jsonPath("$.successCount").value(1))
                .andExpect(jsonPath("$.failureCount").value(1));
    }
}
