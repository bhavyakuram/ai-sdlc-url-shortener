package com.aisdlc.urlshortener.api;

import com.aisdlc.urlshortener.api.dto.AnalyticsResponse;
import com.aisdlc.urlshortener.api.dto.CreateLinkRequest;
import com.aisdlc.urlshortener.api.dto.LinkResponse;
import com.aisdlc.urlshortener.data.ShortLinkEntity;
import com.aisdlc.urlshortener.service.LinkService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints exactly matching step3/api-contract.yaml. Error
 * mapping (400/404/409/410) lives in {@link ApiExceptionHandler} —
 * kept out of this class so it stays focused on the happy path
 * (rules/coding-standards.md separation).
 */
@RestController
public class LinkController {

    private final LinkService linkService;

    public LinkController(LinkService linkService) {
        this.linkService = linkService;
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
}
