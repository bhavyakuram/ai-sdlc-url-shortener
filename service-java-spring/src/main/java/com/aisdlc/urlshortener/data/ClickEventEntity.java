package com.aisdlc.urlshortener.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

/**
 * A single recorded click on a short link. Maps 1:1 to the {@code click_event} table
 * defined in {@code state-migration.md}. {@code referrer} and {@code country} are
 * nullable by design: {@code referrer} is absent when the client sent no {@code Referer}
 * header, {@code country} is null when the geo-IP lookup failed soft (R-7, AC20).
 *
 * <p>Uses a plain {@code shortLinkId} foreign-key column rather than a JPA
 * {@code @ManyToOne} association -- stats aggregation only ever needs the id, so this
 * avoids incidental lazy-loading/association-fetch concerns for a purely analytical row.
 */
@Entity
@Table(name = "click_event")
public class ClickEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "short_link_id", nullable = false)
    private Long shortLinkId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "referrer", length = 512)
    private String referrer;

    @Column(name = "country", length = 2)
    private String country;

    /** Required by JPA. */
    protected ClickEventEntity() {
    }

    public ClickEventEntity(Long shortLinkId, Instant occurredAt, String referrer, String country) {
        this.shortLinkId = Objects.requireNonNull(shortLinkId, "shortLinkId");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
        this.referrer = referrer;
        this.country = country;
    }

    public Long getId() {
        return id;
    }

    public Long getShortLinkId() {
        return shortLinkId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getReferrer() {
        return referrer;
    }

    public String getCountry() {
        return country;
    }
}
