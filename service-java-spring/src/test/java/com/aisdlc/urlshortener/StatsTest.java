package com.aisdlc.urlshortener;

import com.aisdlc.urlshortener.data.ClickEventEntity;
import com.aisdlc.urlshortener.data.ClickEventRepository;
import com.aisdlc.urlshortener.data.ShortLinkEntity;
import com.aisdlc.urlshortener.data.ShortLinkRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET /api/v1/links/{code}/stats} -- AC21 through AC25 (feature-spec.md 3.3,
 * acceptance-criteria.md).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StatsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ShortLinkRepository shortLinkRepository;

    @Autowired
    private ClickEventRepository clickEventRepository;

    /** AC21 -- happy path: 42 clicks across 2 days and 2 countries. */
    @Test
    void statsAggregateAcrossDaysAndCountries() throws Exception {
        Instant now = Instant.now();
        ShortLinkEntity link = shortLinkRepository.saveAndFlush(
                new ShortLinkEntity("abc1234x", "https://example.com/x", now, now.plus(30, ChronoUnit.DAYS)));

        Instant day1 = now.minus(1, ChronoUnit.DAYS);
        Instant day2 = now;
        for (int i = 0; i < 10; i++) {
            clickEventRepository.save(new ClickEventEntity(link.getId(), day1, null, "US"));
        }
        for (int i = 0; i < 32; i++) {
            clickEventRepository.save(new ClickEventEntity(link.getId(), day2, null, null));
        }
        clickEventRepository.flush();

        MvcResult result = mockMvc.perform(get("/api/v1/links/abc1234x/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalClicks").value(42))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        long daySum = 0;
        for (JsonNode day : json.get("clicksByDay")) {
            daySum += day.get("count").asLong();
        }
        long countrySum = 0;
        for (JsonNode country : json.get("clicksByCountry")) {
            countrySum += country.get("count").asLong();
        }
        assertThat(daySum).isEqualTo(42);
        assertThat(countrySum).isEqualTo(42);
    }

    /** AC22 -- zero-click link is not a 404 condition. */
    @Test
    void zeroClickLinkReturns200WithEmptyArrays() throws Exception {
        Instant now = Instant.now();
        shortLinkRepository.saveAndFlush(
                new ShortLinkEntity("fresh01x", "https://example.com/fresh", now, now.plus(30, ChronoUnit.DAYS)));

        mockMvc.perform(get("/api/v1/links/fresh01x/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalClicks").value(0))
                .andExpect(jsonPath("$.clicksByDay").isEmpty())
                .andExpect(jsonPath("$.clicksByCountry").isEmpty());
    }

    /** AC23 -- unknown code. */
    @Test
    void unknownCodeReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/links/neverExistedX/stats"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CODE_NOT_FOUND"));
    }

    /** AC24 -- expired code remains queryable (not 404), unlike the redirect endpoint. */
    @Test
    void expiredCodeStatsRemainQueryable() throws Exception {
        Instant past = Instant.now().minus(1, ChronoUnit.DAYS);
        ShortLinkEntity link = shortLinkRepository.saveAndFlush(
                new ShortLinkEntity("old0002x", "https://example.com/old2", past.minus(30, ChronoUnit.DAYS), past));
        for (int i = 0; i < 5; i++) {
            clickEventRepository.save(new ClickEventEntity(link.getId(), past.minus(1, ChronoUnit.HOURS), null, "US"));
        }
        clickEventRepository.flush();

        mockMvc.perform(get("/api/v1/links/old0002x/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalClicks").value(5));
    }

    /** AC25 -- referrer omitted when absent: click still counted, no error. */
    @Test
    void clickWithNoReferrerIsStillCounted() throws Exception {
        Instant now = Instant.now();
        ShortLinkEntity link = shortLinkRepository.saveAndFlush(
                new ShortLinkEntity("noref01x", "https://example.com/noref", now, now.plus(30, ChronoUnit.DAYS)));
        clickEventRepository.saveAndFlush(new ClickEventEntity(link.getId(), now, null, "US"));

        mockMvc.perform(get("/api/v1/links/noref01x/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalClicks").value(1));
    }
}
