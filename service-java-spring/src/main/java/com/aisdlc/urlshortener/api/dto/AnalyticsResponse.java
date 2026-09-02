package com.aisdlc.urlshortener.api.dto;

import com.aisdlc.urlshortener.service.LinkService.AnalyticsResult;

import java.util.List;

/** Response body for GET /links/{code}/analytics. AC-8. */
public record AnalyticsResponse(String code, long clickCount, List<ClickEventDto> events) {

    public static AnalyticsResponse from(AnalyticsResult result) {
        List<ClickEventDto> events = result.events().stream()
                .map(ClickEventDto::from)
                .toList();
        return new AnalyticsResponse(result.code(), result.clickCount(), events);
    }
}
