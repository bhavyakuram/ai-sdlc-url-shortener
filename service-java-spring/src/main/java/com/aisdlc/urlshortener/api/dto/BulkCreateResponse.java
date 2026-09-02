package com.aisdlc.urlshortener.api.dto;

import java.util.List;

/** Response body for POST /links/bulk — one result per submitted item, same order. */
public record BulkCreateResponse(List<BulkItemResult> results) {
}
