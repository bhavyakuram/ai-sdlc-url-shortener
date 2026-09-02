package com.aisdlc.urlshortener.api;

import com.aisdlc.urlshortener.api.dto.AnalyticsResponse;
import com.aisdlc.urlshortener.api.dto.BulkCreateRequest;
import com.aisdlc.urlshortener.api.dto.BulkCreateResponse;
import com.aisdlc.urlshortener.api.dto.BulkItemResult;
import com.aisdlc.urlshortener.api.dto.CreateLinkRequest;
import com.aisdlc.urlshortener.api.dto.ErrorResponse;
import com.aisdlc.urlshortener.api.dto.LinkResponse;
import com.aisdlc.urlshortener.data.ShortLinkEntity;
import com.aisdlc.urlshortener.service.LinkService;
import com.aisdlc.urlshortener.service.exception.AliasTakenException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * REST endpoints exactly matching step3/api-contract.yaml +
 * step3/api-contract-delta.yaml. Error mapping for the single-item
 * endpoints (400/404/409/410) lives in {@link ApiExceptionHandler} —
 * kept out of this class so it stays focused on the happy path
 * (rules/coding-standards.md separation). {@link #createBulk} is the
 * one exception: its per-item errors are caught here, not in
 * ApiExceptionHandler, because a per-item failure must not become an
 * HTTP-level error for the whole request (FS-5).
 */
@RestController
public class LinkController {

    private final LinkService linkService;
    private final Validator validator;

    public LinkController(LinkService linkService, Validator validator) {
        this.linkService = linkService;
        this.validator = validator;
    }

    @PostMapping("/links")
    public ResponseEntity<LinkResponse> createLink(@Valid @RequestBody CreateLinkRequest request) {
        ShortLinkEntity created = linkService.createLink(
                request.targetUrl(), request.alias(), request.expiresInDays());
        return ResponseEntity.status(HttpStatus.CREATED).body(LinkResponse.from(created));
    }

    @GetMapping("/{code}")
    public ResponseEntity<Void> redirect(
            @PathVariable String code,
            @RequestHeader(value = "Referer", required = false) String referrer) {
        String targetUrl = linkService.resolveAndRecordClick(code, referrer);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, targetUrl)
                .build();
    }

    @GetMapping("/links/{code}/analytics")
    public ResponseEntity<AnalyticsResponse> analytics(@PathVariable String code) {
        var result = linkService.getAnalytics(code);
        return ResponseEntity.ok(AnalyticsResponse.from(result));
    }

    /**
     * AC-10 (all-valid) / AC-11 (mixed/partial-failure) / AC-12 (empty,
     * rejected by {@code @Valid} on the list) / AC-13 (over-limit,
     * same) / AC-14 (bulk collision-safety, via the unchanged
     * {@link LinkService#createLink} DB-unique-constraint path).
     *
     * <p>Deliberately does NOT delegate the per-item loop to
     * {@code LinkService} — see step3/technical-design.md: keeping it
     * here reuses {@code CreateLinkRequest}'s existing validation
     * annotations via a programmatic {@link Validator#validate} call
     * (no regex duplication) without requiring the service layer to
     * import an api-layer DTO, which would violate
     * rules/architecture.md's api -&gt; service -&gt; data dependency
     * direction. {@code LinkService} itself is unmodified by this
     * feature.
     */
    @PostMapping("/links/bulk")
    public ResponseEntity<BulkCreateResponse> createBulk(@Valid @RequestBody BulkCreateRequest request) {
        List<BulkItemResult> results = new ArrayList<>();

        for (CreateLinkRequest item : request.items()) {
            Set<ConstraintViolation<CreateLinkRequest>> violations = validator.validate(item);
            if (!violations.isEmpty()) {
                String message = violations.iterator().next().getMessage();
                results.add(BulkItemResult.error(new ErrorResponse("INVALID_REQUEST", message)));
                continue;
            }
            try {
                ShortLinkEntity created = linkService.createLink(
                        item.targetUrl(), item.alias(), item.expiresInDays());
                results.add(BulkItemResult.created(LinkResponse.from(created)));
            } catch (AliasTakenException e) {
                results.add(BulkItemResult.error(new ErrorResponse("ALIAS_TAKEN", e.getMessage())));
            }
        }

        return ResponseEntity.ok(new BulkCreateResponse(results));
    }
}
