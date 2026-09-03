package com.aisdlc.urlshortener.api;

import com.aisdlc.urlshortener.api.dto.BatchCreateLinkRequest;
import com.aisdlc.urlshortener.api.dto.BatchCreateLinkResponse;
import com.aisdlc.urlshortener.api.dto.BatchItemResult;
import com.aisdlc.urlshortener.api.dto.CreateLinkRequest;
import com.aisdlc.urlshortener.api.dto.LinkResponse;
import com.aisdlc.urlshortener.api.dto.StatsResponse;
import com.aisdlc.urlshortener.api.util.ClientIpResolver;
import com.aisdlc.urlshortener.data.ShortLinkEntity;
import com.aisdlc.urlshortener.service.BulkItemOutcome;
import com.aisdlc.urlshortener.service.BulkLinkItem;
import com.aisdlc.urlshortener.service.BulkLinkOrchestrator;
import com.aisdlc.urlshortener.service.LinkService;
import com.aisdlc.urlshortener.service.LinkStats;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * The three-endpoint API surface (feature-spec.md Section 2). {@code GET /{code}} lives at
 * the root, not under {@code /api/v1}, by deliberate design (a short link must itself be
 * short) -- feature-spec.md Section 1.
 */
@RestController
public class LinkController {

    private final LinkService linkService;
    private final BulkLinkOrchestrator bulkLinkOrchestrator;

    public LinkController(LinkService linkService, BulkLinkOrchestrator bulkLinkOrchestrator) {
        this.linkService = linkService;
        this.bulkLinkOrchestrator = bulkLinkOrchestrator;
    }

    @PostMapping("/api/v1/links")
    public ResponseEntity<LinkResponse> createLink(@RequestBody CreateLinkRequest request,
                                                     HttpServletRequest httpRequest) {
        ShortLinkEntity link = linkService.createLink(request.url(), request.customCode());
        String shortUrl = buildShortUrl(httpRequest, link.getCode());
        LinkResponse body = LinkResponse.from(link, shortUrl);
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/" + link.getCode()))
                .body(body);
    }

    /**
     * Batch-create up to 100 short links in one request (feature-spec.md Section 3.1,
     * FR-B1/FR-B2/FR-B3). Rate limiting (429) for this endpoint is enforced entirely by
     * {@link BatchRateLimitInterceptor} before this method runs, on a separate, IP-only
     * bucket from the redirect path's limiter. Each item is attempted independently via
     * {@link #bulkLinkOrchestrator} -- through the injected {@link LinkService} proxy, never
     * a self-invocation (R-BULK-2) -- so one item's failure never affects another's outcome
     * or persistence (R-BULK-4). Always {@code 200 OK} once whole-request validation passes;
     * {@link com.aisdlc.urlshortener.service.exception.EmptyBatchException}/{@link
     * com.aisdlc.urlshortener.service.exception.BatchTooLargeException} thrown by {@link
     * #bulkLinkOrchestrator} propagate uncaught to {@link ApiExceptionHandler}, exactly like
     * every other whole-request validation failure in this codebase.
     */
    @PostMapping("/api/v1/links/batch")
    public ResponseEntity<BatchCreateLinkResponse> createBatch(@RequestBody BatchCreateLinkRequest request,
                                                                  HttpServletRequest httpRequest) {
        List<CreateLinkRequest> requestItems = request.items() != null ? request.items() : List.of();
        List<BulkLinkItem> items = requestItems.stream()
                .map(i -> new BulkLinkItem(i.url(), i.customCode()))
                .toList();
        List<BulkItemOutcome> outcomes = bulkLinkOrchestrator.processBatch(items);

        List<BatchItemResult> results = outcomes.stream()
                .map(o -> o.isSuccess()
                        ? BatchItemResult.created(o.link(), buildShortUrl(httpRequest, o.link().getCode()))
                        : BatchItemResult.failed(o.errorCode(), o.errorMessage()))
                .toList();
        long successCount = results.stream().filter(r -> "CREATED".equals(r.status())).count();

        return ResponseEntity.ok(new BatchCreateLinkResponse(results, (int) successCount,
                results.size() - (int) successCount));
    }

    /**
     * Rate limiting (429) for this endpoint is enforced entirely by {@link
     * RateLimitInterceptor} before this method runs -- it never sees a request past the
     * (IP, code) bucket's limit.
     */
    @GetMapping("/{code}")
    public ResponseEntity<Void> redirect(@PathVariable String code, HttpServletRequest request) {
        String referrer = request.getHeader("Referer");
        String sourceIp = ClientIpResolver.resolve(request);
        ShortLinkEntity link = linkService.redirectAndRecordClick(code, referrer, sourceIp);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(link.getTargetUrl()))
                .build();
    }

    @GetMapping("/api/v1/links/{code}/stats")
    public ResponseEntity<StatsResponse> stats(@PathVariable String code) {
        LinkStats stats = linkService.getStats(code);
        return ResponseEntity.ok(StatsResponse.from(stats));
    }

    private String buildShortUrl(HttpServletRequest request, String code) {
        return ServletUriComponentsBuilder.fromRequestUri(request)
                .replacePath("/" + code)
                .replaceQuery(null)
                .build()
                .toUriString();
    }
}
