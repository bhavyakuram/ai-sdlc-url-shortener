package com.aisdlc.urlshortener.api.dto;

import com.aisdlc.urlshortener.service.LinkStats;

import java.time.Instant;
import java.util.List;

/** Response body for {@code GET /api/v1/links/{code}/stats} (feature-spec.md 3.3). */
public record StatsResponse(
        String shortCode,
        String longUrl,
        Instant createdAt,
        Instant expiresAt,
        long totalClicks,
        List<ClicksByDay> clicksByDay,
        List<ClicksByCountry> clicksByCountry
) {

    public record ClicksByDay(String date, long count) {
    }

    public record ClicksByCountry(String country, long count) {
    }

    public static StatsResponse from(LinkStats stats) {
        List<ClicksByDay> byDay = stats.clicksByDay().stream()
                .map(d -> new ClicksByDay(d.date(), d.count()))
                .toList();
        List<ClicksByCountry> byCountry = stats.clicksByCountry().stream()
                .map(c -> new ClicksByCountry(c.country(), c.count()))
                .toList();
        return new StatsResponse(stats.shortCode(), stats.longUrl(), stats.createdAt(), stats.expiresAt(),
                stats.totalClicks(), byDay, byCountry);
    }
}
