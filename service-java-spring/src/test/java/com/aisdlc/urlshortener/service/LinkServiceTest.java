package com.aisdlc.urlshortener.service;

import com.aisdlc.urlshortener.data.ShortLinkEntity;
import com.aisdlc.urlshortener.data.ShortLinkRepository;
import com.aisdlc.urlshortener.service.exception.AliasTakenException;
import com.aisdlc.urlshortener.service.exception.LinkExpiredException;
import com.aisdlc.urlshortener.service.exception.LinkNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit-level tests against the real H2-backed service layer. Every
 * test method is tagged with the AC id it verifies, per
 * rules/testing.md Every-AC-Needs-a-Test.
 */
@SpringBootTest
class LinkServiceTest {

    @Autowired
    private LinkService linkService;

    @Autowired
    private ShortLinkRepository shortLinkRepository;

    @Test
    // AC-1: create with generated code
    void createLink_withoutAlias_generatesUniqueCode() {
        ShortLinkEntity link = linkService.createLink("https://example.com/a", null, null);

        assertThat(link.getShortCode()).isNotBlank();
        assertThat(link.getShortCode()).isNotEqualTo("__pending__");
        assertThat(link.getTargetUrl()).isEqualTo("https://example.com/a");
    }

    @Test
    // AC-2: create with custom alias
    void createLink_withAlias_usesExactAlias() {
        ShortLinkEntity link = linkService.createLink("https://example.com/b", "my-alias-1", null);

        assertThat(link.getShortCode()).isEqualTo("my-alias-1");
    }

    @Test
    // AC-3: alias collision -> AliasTakenException, existing link unchanged
    void createLink_withTakenAlias_throwsAliasTaken() {
        linkService.createLink("https://example.com/c1", "dup-alias", null);

        assertThrows(AliasTakenException.class,
                () -> linkService.createLink("https://example.com/c2", "dup-alias", null));

        ShortLinkEntity existing = shortLinkRepository.findByShortCode("dup-alias").orElseThrow();
        assertThat(existing.getTargetUrl()).isEqualTo("https://example.com/c1");
    }

    @Test
    // AC-5: successful resolve records a click and returns the target
    void resolveAndRecordClick_existingLink_returnsTargetAndRecordsClick() {
        ShortLinkEntity link = linkService.createLink("https://example.com/d", "resolve-ok", null);

        String target = linkService.resolveAndRecordClick("resolve-ok", "https://twitter.com");

        assertThat(target).isEqualTo("https://example.com/d");
        var analytics = linkService.getAnalytics("resolve-ok");
        assertThat(analytics.clickCount()).isEqualTo(1);
        assertThat(analytics.events().get(0).getReferrer()).isEqualTo("https://twitter.com");
    }

    @Test
    // AC-6: unknown code -> LinkNotFoundException, no click recorded
    void resolveAndRecordClick_unknownCode_throwsNotFound() {
        assertThrows(LinkNotFoundException.class,
                () -> linkService.resolveAndRecordClick("does-not-exist", null));
    }

    @Test
    // AC-7: expired code -> LinkExpiredException
    void resolveAndRecordClick_expiredCode_throwsExpired() {
        linkService.createLink("https://example.com/e", "expired-link", -1);
        // expiresInDays=-1 -> expiresAt is in the past already

        assertThrows(LinkExpiredException.class,
                () -> linkService.resolveAndRecordClick("expired-link", null));
    }

    @Test
    // AC-8: analytics report exactly N clicks with N event-log entries
    void getAnalytics_reflectsExactClickCount() {
        linkService.createLink("https://example.com/f", "analytics-check", null);

        linkService.resolveAndRecordClick("analytics-check", "a.com");
        linkService.resolveAndRecordClick("analytics-check", null);
        linkService.resolveAndRecordClick("analytics-check", "b.com");

        var analytics = linkService.getAnalytics("analytics-check");
        assertThat(analytics.clickCount()).isEqualTo(3);
        assertThat(analytics.events()).hasSize(3);
    }

    @Test
    // AC-9: concurrent generated-code creations never collide
    void createLink_concurrentCreation_neverCollides() throws Exception {
        int n = 20;
        ExecutorService pool = Executors.newFixedThreadPool(8);
        List<Callable<String>> tasks = IntStream.range(0, n)
                .<Callable<String>>mapToObj(i -> () ->
                        linkService.createLink("https://example.com/concurrent/" + i, null, null).getShortCode())
                .collect(Collectors.toList());

        List<Future<String>> futures = pool.invokeAll(tasks);
        Set<String> codes = new HashSet<>();
        for (Future<String> f : futures) {
            codes.add(f.get());
        }
        pool.shutdown();

        assertThat(codes).hasSize(n); // no duplicates among n concurrently-generated codes
    }
}
