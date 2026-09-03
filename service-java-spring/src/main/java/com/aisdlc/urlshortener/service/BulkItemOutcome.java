package com.aisdlc.urlshortener.service;

import com.aisdlc.urlshortener.data.ShortLinkEntity;

/**
 * Service-layer-owned per-item outcome of {@link BulkLinkOrchestrator#processBatch}: either a
 * successfully persisted {@link ShortLinkEntity}, or an {@code (errorCode, errorMessage)}
 * pair. Carries zero HTTP types -- in particular no {@code shortUrl}, which is an
 * {@code HttpServletRequest}-derived concept built by {@code api.LinkController} afterward,
 * exactly the same precedent {@link LinkService#createLink} already sets for the
 * single-create path (technical-design.md Section 5.2).
 */
public record BulkItemOutcome(ShortLinkEntity link, String errorCode, String errorMessage) {

    public static BulkItemOutcome success(ShortLinkEntity link) {
        return new BulkItemOutcome(link, null, null);
    }

    public static BulkItemOutcome failure(String errorCode, String errorMessage) {
        return new BulkItemOutcome(null, errorCode, errorMessage);
    }

    public boolean isSuccess() {
        return link != null;
    }
}
