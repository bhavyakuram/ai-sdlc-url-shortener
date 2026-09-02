package com.aisdlc.urlshortener.api.dto;

/**
 * One slot in a bulk-create response — either a created link or an
 * error, never both. Exactly one of {@code link} / {@code error} is
 * non-null depending on {@code status}.
 */
public record BulkItemResult(String status, LinkResponse link, ErrorResponse error) {

    public static final String STATUS_CREATED = "created";
    public static final String STATUS_ERROR = "error";

    public static BulkItemResult created(LinkResponse link) {
        return new BulkItemResult(STATUS_CREATED, link, null);
    }

    public static BulkItemResult error(ErrorResponse error) {
        return new BulkItemResult(STATUS_ERROR, null, error);
    }
}
