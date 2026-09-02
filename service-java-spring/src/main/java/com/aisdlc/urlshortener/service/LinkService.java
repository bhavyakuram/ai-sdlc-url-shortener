package com.aisdlc.urlshortener.service;

import com.aisdlc.urlshortener.data.ClickEventEntity;
import com.aisdlc.urlshortener.data.ClickEventRepository;
import com.aisdlc.urlshortener.data.ShortLinkEntity;
import com.aisdlc.urlshortener.data.ShortLinkRepository;
import com.aisdlc.urlshortener.service.exception.AliasTakenException;
import com.aisdlc.urlshortener.service.exception.LinkExpiredException;
import com.aisdlc.urlshortener.service.exception.LinkNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Core business logic for creating links, resolving redirects (with
 * click recording), and reporting analytics. See
 * step3/technical-design.md for the design this implements and
 * step2/acceptance-criteria.md for the AC ids referenced in comments.
 */
@Service
public class LinkService {

    private final ShortLinkRepository shortLinkRepository;
    private final ClickEventRepository clickEventRepository;
    private final CodeGenerator codeGenerator;
    private final int defaultExpiryDays;

    public LinkService(
            ShortLinkRepository shortLinkRepository,
            ClickEventRepository clickEventRepository,
            CodeGenerator codeGenerator,
            @Value("${app.short-link.default-expiry-days}") int defaultExpiryDays) {
        this.shortLinkRepository = shortLinkRepository;
        this.clickEventRepository = clickEventRepository;
        this.codeGenerator = codeGenerator;
        this.defaultExpiryDays = defaultExpiryDays;
    }

    /**
     * Creates a short link. AC-1 (generated code) / AC-2 (custom alias) /
     * AC-3 (alias collision -> AliasTakenException) / AC-9
     * (collision-safety under concurrency).
     */
    @Transactional
    public ShortLinkEntity createLink(String targetUrl, String alias, Integer expiresInDays) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(
                86400L * (expiresInDays != null ? expiresInDays : defaultExpiryDays));

        if (alias != null && !alias.isBlank()) {
            // AC-2 / AC-3: custom alias path. Let the DB unique
            // constraint arbitrate the race (technical-design.md);
            // don't pre-check-then-insert (that's a race condition,
            // not a fix for one).
            ShortLinkEntity entity = new ShortLinkEntity(alias, targetUrl, now, expiresAt);
            try {
                return shortLinkRepository.save(entity);
            } catch (DataIntegrityViolationException e) {
                throw new AliasTakenException(alias);
            }
        }

        // AC-1 / AC-9: generated-code path. Persist first (with a
        // temporary placeholder code), then derive the real code from
        // the assigned id, then update in the same transaction. The
        // id is DB-assigned and unique by construction, so there is no
        // collision to retry on this path.
        ShortLinkEntity entity = new ShortLinkEntity("__pending__", targetUrl, now, expiresAt);
        ShortLinkEntity saved = shortLinkRepository.save(entity);
        saved.setShortCode(codeGenerator.encode(saved.getId()));
        return shortLinkRepository.save(saved);
    }

    /**
     * Resolves a short code to its target and records a click.
     * AC-5 (success) / AC-6 (LinkNotFoundException) / AC-7
     * (LinkExpiredException).
     */
    @Transactional
    public String resolveAndRecordClick(String code, String referrer) {
        ShortLinkEntity link = shortLinkRepository.findByShortCode(code)
                .orElseThrow(() -> new LinkNotFoundException(code));

        Instant now = Instant.now();
        if (link.isExpired(now)) {
            throw new LinkExpiredException(code);
        }

        clickEventRepository.save(new ClickEventEntity(link.getId(), now, referrer));
        return link.getTargetUrl();
    }

    /** AC-8: analytics reflect exactly the recorded clicks. */
    public AnalyticsResult getAnalytics(String code) {
        ShortLinkEntity link = shortLinkRepository.findByShortCode(code)
                .orElseThrow(() -> new LinkNotFoundException(code));

        List<ClickEventEntity> events =
                clickEventRepository.findByShortLinkIdOrderByOccurredAtDesc(link.getId());

        return new AnalyticsResult(code, events.size(), events);
    }

    /** Simple value holder returned to the API layer. */
    public record AnalyticsResult(String code, long clickCount, List<ClickEventEntity> events) {
    }
}
