package com.aisdlc.urlshortener.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A single recorded click on a {@link ShortLinkEntity}. {@code referrer}
 * is nullable — browsers do not reliably send a Referer header
 * (see step1/risk-register.md R5), and the schema tolerates that
 * rather than treating it as an error.
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

    protected ClickEventEntity() {
        // JPA requires a no-arg constructor
    }

    public ClickEventEntity(Long shortLinkId, Instant occurredAt, String referrer) {
        this.shortLinkId = shortLinkId;
        this.occurredAt = occurredAt;
        this.referrer = referrer;
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
}
