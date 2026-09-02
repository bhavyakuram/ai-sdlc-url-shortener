package com.aisdlc.urlshortener.api.dto;

import com.aisdlc.urlshortener.data.ClickEventEntity;

import java.time.Instant;

/** A single click-event row in an analytics response. */
public record ClickEventDto(Instant timestamp, String referrer) {

    public static ClickEventDto from(ClickEventEntity entity) {
        return new ClickEventDto(entity.getOccurredAt(), entity.getReferrer());
    }
}
