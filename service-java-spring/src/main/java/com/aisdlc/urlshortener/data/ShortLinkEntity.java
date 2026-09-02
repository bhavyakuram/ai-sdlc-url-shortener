package com.aisdlc.urlshortener.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * A short link record. The uniqueness constraint on {@code shortCode}
 * is the concurrency-safety mechanism for AC-9 (see
 * step3/technical-design.md) — the database, not application code,
 * arbitrates collisions.
 */
@Entity
@Table(name = "short_link", uniqueConstraints = @UniqueConstraint(columnNames = "short_code"))
public class ShortLinkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "short_code", nullable = false, length = 32)
    private String shortCode;

    @Column(name = "target_url", nullable = false, length = 2048)
    private String targetUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected ShortLinkEntity() {
        // JPA requires a no-arg constructor
    }

    public ShortLinkEntity(String shortCode, String targetUrl, Instant createdAt, Instant expiresAt) {
        this.shortCode = shortCode;
        this.targetUrl = targetUrl;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public Long getId() {
        return id;
    }

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isExpired(Instant now) {
        return expiresAt.isBefore(now);
    }
}
