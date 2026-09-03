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
 * A persisted short link. Maps 1:1 to the {@code short_link} table defined in
 * {@code state-migration.md}.
 *
 * <p>Data layer: no HTTP/api-layer imports here, per {@code rules/architecture.md}
 * Dependency Direction ({@code api -> service -> data}, never the reverse).
 */
@Entity
@Table(name = "short_link")
public class ShortLinkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 32)
    private String code;

    @Column(name = "target_url", nullable = false, length = 2048)
    private String targetUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** Required by JPA. */
    protected ShortLinkEntity() {
    }

    public ShortLinkEntity(String code, String targetUrl, Instant createdAt, Instant expiresAt) {
        this.code = Objects.requireNonNull(code, "code");
        this.targetUrl = Objects.requireNonNull(targetUrl, "targetUrl");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
    }

    public boolean isExpired(Instant now) {
        return expiresAt.isBefore(now);
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    /** Mutable for Candidate A's generated-code path (insert-then-derive-then-update). */
    public void setCode(String code) {
        this.code = Objects.requireNonNull(code, "code");
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
}
