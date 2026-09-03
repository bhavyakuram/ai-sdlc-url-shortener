package com.aisdlc.urlshortener.service;

/**
 * Service-layer-owned {@code (url, customCode)} pair -- the parameter type {@link
 * BulkLinkOrchestrator#processBatch} actually accepts, deliberately <b>not</b> {@code
 * api.dto.CreateLinkRequest} (technical-design.md Section 5.1). {@code api.dto} sits one
 * layer above {@code service} in the {@code api -> service -> data} dependency direction
 * (rules/architecture.md Dependency Direction), so {@code service} must never import it --
 * this record exists so it never has to. {@code api.LinkController} performs the one-line
 * translation from {@code CreateLinkRequest} to this type at the layer boundary.
 */
public record BulkLinkItem(String url, String customCode) {
}
