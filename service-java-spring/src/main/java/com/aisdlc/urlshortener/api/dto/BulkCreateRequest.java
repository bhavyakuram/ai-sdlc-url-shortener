package com.aisdlc.urlshortener.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request body for POST /links/bulk. Only the list itself is
 * whole-request validated (empty / over-limit -> 400, AC-12/AC-13).
 *
 * <p>Deliberately NO {@code @Valid} on the element type — see
 * step3/technical-design.md "No Cascading Bean Validation on Items".
 * Spring's cascading validation would reject the whole request if any
 * single item were invalid, which contradicts FS-5/AC-11 (one bad item
 * must not sink the batch). {@link com.aisdlc.urlshortener.api.LinkController#createBulk}
 * validates each item independently instead, item by item.
 */
public record BulkCreateRequest(

        @NotEmpty(message = "items must not be empty")
        @Size(max = 20, message = "items must not exceed 20 entries")
        List<CreateLinkRequest> items
) {
}
