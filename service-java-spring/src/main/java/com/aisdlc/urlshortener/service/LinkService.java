package com.aisdlc.urlshortener.service;

import com.aisdlc.urlshortener.data.ClickEventEntity;
import com.aisdlc.urlshortener.data.ClickEventRepository;
import com.aisdlc.urlshortener.data.ShortLinkEntity;
import com.aisdlc.urlshortener.data.ShortLinkRepository;
import com.aisdlc.urlshortener.service.exception.CodeSpaceExhaustedException;
import com.aisdlc.urlshortener.service.exception.CustomCodeTakenException;
import com.aisdlc.urlshortener.service.exception.InvalidCustomCodeShapeException;
import com.aisdlc.urlshortener.service.exception.InvalidUrlException;
import com.aisdlc.urlshortener.service.exception.LinkUnavailableException;
import com.aisdlc.urlshortener.service.exception.ReservedCodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Core business logic: create/resolve/stats. No HTTP or Jackson types here -- service layer
 * never imports from {@code api} (rules/architecture.md Dependency Direction).
 */
@Service
public class LinkService {

    private static final Logger log = LoggerFactory.getLogger(LinkService.class);

    private static final int EXPIRY_DAYS = 30;
    private static final int MAX_URL_LENGTH = 2048;
    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    private static final int MIN_CUSTOM_CODE_LENGTH = 3;
    private static final int MAX_CUSTOM_CODE_LENGTH = 32;
    private static final Pattern BASE62_PATTERN = Pattern.compile("^[0-9A-Za-z]+$");

    /** feature-spec.md Section 5 -- fixed reserved-word set, checked before uniqueness. */
    private static final Set<String> RESERVED_CODES = Set.of("api", "actuator", "health", "favicon.ico");

    private final ShortLinkRepository shortLinkRepository;
    private final ClickEventRepository clickEventRepository;
    private final CodeGenerator codeGenerator;
    private final GeoLookupService geoLookupService;

    public LinkService(ShortLinkRepository shortLinkRepository,
                        ClickEventRepository clickEventRepository,
                        CodeGenerator codeGenerator,
                        GeoLookupService geoLookupService) {
        this.shortLinkRepository = shortLinkRepository;
        this.clickEventRepository = clickEventRepository;
        this.codeGenerator = codeGenerator;
        this.geoLookupService = geoLookupService;
    }

    /**
     * Creates a short link. If {@code customCode} is present (and valid, not reserved, not
     * taken) it is used verbatim (FR-8); otherwise a server-generated 7-char base62 code is
     * allocated via Candidate A (insert-then-derive-then-update, one transaction -- see
     * {@link #createWithGeneratedCode}).
     */
    @Transactional
    public ShortLinkEntity createLink(String url, String customCode) {
        validateUrl(url);
        Instant now = Instant.now();
        Instant expiresAt = now.plus(EXPIRY_DAYS, ChronoUnit.DAYS);

        if (customCode != null && !customCode.isBlank()) {
            validateCustomCodeShape(customCode);
            validateNotReserved(customCode);
            return createWithCustomCode(customCode, url, now, expiresAt);
        }
        return createWithGeneratedCode(url, now, expiresAt);
    }

    /**
     * Candidate A (technical-design.md, parallel-explorer-candidates.md): insert a row with
     * a unique placeholder code to obtain a DB-assigned identity id (atomic by construction
     * -- two concurrent requests can never receive the same id, which is what makes this
     * satisfy AC10 structurally rather than probabilistically), derive the base62 code from
     * that id, then update the row's code -- all in one transaction.
     */
    private ShortLinkEntity createWithGeneratedCode(String url, Instant now, Instant expiresAt) {
        ShortLinkEntity entity = new ShortLinkEntity(codeGenerator.randomPlaceholder(), url, now, expiresAt);
        try {
            entity = shortLinkRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException ex) {
            // Vanishingly unlikely (32-char random UUID-derived placeholder), but never
            // silently swallowed -- rules/coding-standards.md No Silent Catches.
            throw new CodeSpaceExhaustedException("Unable to allocate a placeholder code", ex);
        }

        String derivedCode = codeGenerator.encode(entity.getId());
        entity.setCode(derivedCode);
        try {
            return shortLinkRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException ex) {
            // Pathological: base62(id) collided with a pre-existing *custom* code (the id
            // itself is guaranteed unique by the DB identity column, so this can only happen
            // against a manually-chosen custom code equal to this id's encoding -- not
            // against another generated code). Not retried within this transaction: a failed
            // flush leaves the Hibernate persistence context unusable for further writes in
            // the same transaction, so retrying here would risk a broken session rather than
            // a clean retry. Surfaced as the documented 503 edge case instead
            // (feature-spec.md 3.1, CODE_SPACE_EXHAUSTED) -- safe for a client to retry.
            throw new CodeSpaceExhaustedException(
                    "Short code space collision deriving a code for this request; retry", ex);
        }
    }

    /** Custom-code path: insert directly, DB unique constraint enforces first-come-first-served. */
    private ShortLinkEntity createWithCustomCode(String customCode, String url, Instant now, Instant expiresAt) {
        ShortLinkEntity entity = new ShortLinkEntity(customCode, url, now, expiresAt);
        try {
            return shortLinkRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException ex) {
            // Insert-then-catch, never check-then-insert (risk-register.md R-2 / AC09) --
            // the DB unique constraint is the sole serialization point, so this is race-safe
            // under real concurrency, unlike a SELECT-then-INSERT check.
            throw new CustomCodeTakenException("Custom code already in use: " + customCode, ex);
        }
    }

    /**
     * Resolves {@code code} to its target link and records exactly one {@link
     * ClickEventEntity} for a successful resolution (AC14). Expiry is checked lazily, at
     * read time (technical-design.md Expiry) -- an expired code and a never-existed code
     * both raise {@link LinkUnavailableException} (feature-spec.md Section 4).
     *
     * <p>Geo-IP lookup failing soft (R-7, AC20) never blocks or fails this method: {@link
     * GeoLookupService#lookupCountry(String)} itself never throws, and a null result is
     * recorded as a null {@code country} (surfaced as {@code "unknown"} at the stats layer).
     *
     * <p>AC41/AC42 (url-shortener-analytics-reliability, technical-design.md): the click
     * write is isolated in its own try/catch, scoped to start only after the lookup/expiry
     * checks above -- {@link LinkUnavailableException} must still propagate uncaught. Uses
     * {@code saveAndFlush}, not {@code save} -- a plain {@code save} defers the physical
     * INSERT to transaction-commit time (after this method returns, inside the
     * {@code @Transactional} AOP interceptor), so a try/catch around it would catch nothing
     * for the actual failure mode (lock timeout / pool exhaustion under a concurrent burst).
     */
    @Transactional
    public ShortLinkEntity redirectAndRecordClick(String code, String referrer, String sourceIp) {
        ShortLinkEntity link = shortLinkRepository.findByCode(code)
                .orElseThrow(() -> new LinkUnavailableException("No active short link for code: " + code));
        Instant now = Instant.now();
        if (link.isExpired(now)) {
            throw new LinkUnavailableException("Short link has expired: " + code);
        }

        String country = geoLookupService.lookupCountry(sourceIp);
        ClickEventEntity click = new ClickEventEntity(link.getId(), now, referrer, country);
        try {
            clickEventRepository.saveAndFlush(click);
        } catch (RuntimeException ex) {
            log.error("Failed to record click for code '{}' (linkId={}, occurredAt={}) -- "
                    + "redirect will proceed without recording this click.",
                    code, link.getId(), now, ex);
        }

        return link;
    }

    /**
     * Stats remain queryable after expiry (AC24) -- expiry only gates the redirect path.
     * The only 404 condition here is "code was never created" (feature-spec.md 3.3).
     */
    @Transactional(readOnly = true)
    public LinkStats getStats(String code) {
        ShortLinkEntity link = shortLinkRepository.findByCode(code)
                .orElseThrow(() -> new LinkUnavailableException("No short link for code: " + code));

        List<ClickEventEntity> clicks = clickEventRepository.findByShortLinkId(link.getId());

        TreeMap<String, Long> byDay = new TreeMap<>();
        TreeMap<String, Long> byCountry = new TreeMap<>();
        for (ClickEventEntity click : clicks) {
            String day = click.getOccurredAt().atZone(ZoneOffset.UTC).toLocalDate().toString();
            byDay.merge(day, 1L, Long::sum);

            String country = click.getCountry();
            String countryKey = (country == null || country.isBlank()) ? "unknown" : country;
            byCountry.merge(countryKey, 1L, Long::sum);
        }

        List<LinkStats.DailyCount> dailyCounts = byDay.entrySet().stream()
                .map(e -> new LinkStats.DailyCount(e.getKey(), e.getValue()))
                .toList();
        List<LinkStats.CountryCount> countryCounts = byCountry.entrySet().stream()
                .map(e -> new LinkStats.CountryCount(e.getKey(), e.getValue()))
                .toList();

        return new LinkStats(link.getCode(), link.getTargetUrl(), link.getCreatedAt(), link.getExpiresAt(),
                clicks.size(), dailyCounts, countryCounts);
    }

    private void validateUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new InvalidUrlException("URL_MALFORMED", "url must not be blank");
        }
        if (url.length() > MAX_URL_LENGTH) {
            throw new InvalidUrlException("URL_TOO_LONG", "url exceeds " + MAX_URL_LENGTH + " characters");
        }
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException ex) {
            throw new InvalidUrlException("URL_MALFORMED", "url is not a well-formed URI");
        }
        String scheme = uri.getScheme();
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase(Locale.ROOT))) {
            throw new InvalidUrlException("INVALID_URL_SCHEME", "URL must use http or https scheme");
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new InvalidUrlException("URL_MALFORMED", "url must include a host");
        }
    }

    private void validateCustomCodeShape(String customCode) {
        if (customCode.length() < MIN_CUSTOM_CODE_LENGTH
                || customCode.length() > MAX_CUSTOM_CODE_LENGTH
                || !BASE62_PATTERN.matcher(customCode).matches()) {
            throw new InvalidCustomCodeShapeException(
                    "customCode must be 3-32 base62 ([0-9A-Za-z]) characters");
        }
    }

    private void validateNotReserved(String customCode) {
        if (RESERVED_CODES.contains(customCode)) {
            throw new ReservedCodeException("customCode '" + customCode + "' is a reserved path segment");
        }
    }
}
