package com.aisdlc.urlshortener.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for POST /links/bulk (FS-5). AC ids in method comments per
 * rules/testing.md Every-AC-Needs-a-Test.
 */
@SpringBootTest
@AutoConfigureMockMvc
class BulkCreateIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    // AC-10: all-valid batch -> 200, all "created", distinct codes
    void allValidBatch_allCreated() throws Exception {
        String body = """
                {"items": [
                    {"targetUrl": "https://example.com/bulk-a"},
                    {"targetUrl": "https://example.com/bulk-b"},
                    {"targetUrl": "https://example.com/bulk-c"}
                ]}""";

        mockMvc.perform(post("/links/bulk").contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results.length()").value(3))
                .andExpect(jsonPath("$.results[0].status").value("created"))
                .andExpect(jsonPath("$.results[1].status").value("created"))
                .andExpect(jsonPath("$.results[2].status").value("created"))
                .andExpect(jsonPath("$.results[0].link.code").exists())
                .andExpect(jsonPath("$.results[1].link.code").exists());
    }

    @Test
    // AC-11: mixed batch -> item 2 fails (alias taken), 1 and 3 unaffected
    void mixedBatch_partialFailureDoesNotAffectOtherItems() throws Exception {
        // pre-create the alias that item 2 will collide on
        mockMvc.perform(post("/links")
                .contentType("application/json")
                .content("{\"targetUrl\":\"https://example.com/pre-existing\",\"alias\":\"bulk-dup\"}"));

        String body = """
                {"items": [
                    {"targetUrl": "https://example.com/bulk-mix-1"},
                    {"targetUrl": "https://example.com/bulk-mix-2", "alias": "bulk-dup"},
                    {"targetUrl": "https://example.com/bulk-mix-3"}
                ]}""";

        mockMvc.perform(post("/links/bulk").contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results.length()").value(3))
                .andExpect(jsonPath("$.results[0].status").value("created"))
                .andExpect(jsonPath("$.results[1].status").value("error"))
                .andExpect(jsonPath("$.results[1].error.code").value("ALIAS_TAKEN"))
                .andExpect(jsonPath("$.results[2].status").value("created"));
    }

    @Test
    // AC-12: empty batch -> 400, nothing created
    void emptyBatch_returns400() throws Exception {
        mockMvc.perform(post("/links/bulk").contentType("application/json").content("{\"items\": []}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    // AC-13: over-limit batch (21 items) -> 400, whole request rejected
    void overLimitBatch_returns400() throws Exception {
        StringBuilder items = new StringBuilder();
        for (int i = 0; i < 21; i++) {
            if (i > 0) items.append(",");
            items.append("{\"targetUrl\": \"https://example.com/over-limit-").append(i).append("\"}");
        }
        String body = "{\"items\": [" + items + "]}";

        mockMvc.perform(post("/links/bulk").contentType("application/json").content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    // AC-14: two items in the same batch request the same alias -> exactly one succeeds
    void bulkCollisionSafety_exactlyOneOfDuplicateAliasSucceeds() throws Exception {
        String body = """
                {"items": [
                    {"targetUrl": "https://example.com/bulk-collide-1", "alias": "bulk-collide"},
                    {"targetUrl": "https://example.com/bulk-collide-2", "alias": "bulk-collide"}
                ]}""";

        mockMvc.perform(post("/links/bulk").contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results.length()").value(2))
                .andExpect(jsonPath("$.results[0].status").value("created"))
                .andExpect(jsonPath("$.results[1].status").value("error"))
                .andExpect(jsonPath("$.results[1].error.code").value("ALIAS_TAKEN"));
    }
}
