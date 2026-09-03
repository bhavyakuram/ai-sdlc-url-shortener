package com.aisdlc.urlshortener.service;

import java.time.Instant;
import java.util.List;

/**
 * Plain service-layer result type for {@code GET /api/v1/links/{code}/stats} -- no Jackson
 * or Spring web annotations here, so the api layer's {@code StatsResponse} DTO maps *from*
 * this, never the other way around (api -> service, never service -> api).
 */
public record LinkStats(
        String shortCode,
        String longUrl,
        Instant createdAt,
        Instant expiresAt,
        long totalClicks,
        List<DailyCount> clicksByDay,
        List<CountryCount> clicksByCountry
) {

    public record DailyCount(String date, long count) {
    }

    public record CountryCount(String country, long count) {
    }
}
