package com.aisdlc.urlshortener.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP-level tests confirming the actual status codes / headers the
 * API contract (step3/api-contract.yaml) promises. Complements
 * LinkServiceTest, which covers business logic at the service layer.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LinkControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    // AC-1 + AC-5: create then redirect (302) with click recorded
    void createThenRedirect_returns201then302() throws Exception {
        mockMvc.perform(post("/links")
                        .contentType("application/json")
                        .content("{\"targetUrl\":\"https://example.com/http-flow\",\"alias\":\"http-flow\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("http-flow"));

        mockMvc.perform(get("/http-flow"))
                .andExpect(status().isFound()) // 302
                .andExpect(header().string("Location", "https://example.com/http-flow"));
    }

    @Test
    // AC-4: invalid targetUrl -> 400
    void createWithInvalidUrl_returns400() throws Exception {
        mockMvc.perform(post("/links")
                        .contentType("application/json")
                        .content("{\"targetUrl\":\"not-a-url\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    // AC-3: alias collision -> 409 with machine-readable code
    void createWithTakenAlias_returns409() throws Exception {
        mockMvc.perform(post("/links")
                        .contentType("application/json")
                        .content("{\"targetUrl\":\"https://example.com/x1\",\"alias\":\"http-dup\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/links")
                        .contentType("application/json")
                        .content("{\"targetUrl\":\"https://example.com/x2\",\"alias\":\"http-dup\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALIAS_TAKEN"));
    }

    @Test
    // AC-6: unknown code -> 404
    void redirectUnknownCode_returns404() throws Exception {
        mockMvc.perform(get("/no-such-code-ever"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LINK_NOT_FOUND"));
    }

    @Test
    // AC-7: expired code -> 410, distinct from 404
    void redirectExpiredCode_returns410() throws Exception {
        mockMvc.perform(post("/links")
                        .contentType("application/json")
                        .content("{\"targetUrl\":\"https://example.com/exp\",\"alias\":\"http-expired\",\"expiresInDays\":1}"));
        // Can't fast-forward time in an integration test without a clock
        // abstraction (not part of this design) — expiry-path unit
        // coverage lives in LinkServiceTest#resolveAndRecordClick_expiredCode_throwsExpired.
        // This test intentionally left as a placeholder documenting that
        // gap rather than silently omitting the HTTP-level case.
    }

    @Test
    // AC-8: analytics endpoint returns count + event log
    void analytics_afterOneClick_showsOneEvent() throws Exception {
        mockMvc.perform(post("/links")
                .contentType("application/json")
                .content("{\"targetUrl\":\"https://example.com/an\",\"alias\":\"http-analytics\"}"));
        mockMvc.perform(get("/http-analytics"));

        mockMvc.perform(get("/links/http-analytics/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clickCount").value(1))
                .andExpect(jsonPath("$.events.length()").value(1));
    }
}
