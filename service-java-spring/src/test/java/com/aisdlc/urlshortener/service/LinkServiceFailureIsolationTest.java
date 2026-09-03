package com.aisdlc.urlshortener.service;

import com.aisdlc.urlshortener.data.ClickEventRepository;
import com.aisdlc.urlshortener.data.ShortLinkEntity;
import com.aisdlc.urlshortener.data.ShortLinkRepository;
import com.aisdlc.urlshortener.service.exception.LinkUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * url-shortener-analytics-reliability, AC41/AC42. Plain-Mockito unit test
 * (no Spring context) that FORCES the click-write to throw, rather than
 * trusting the try/catch by inspection -- same pattern this project used
 * for the equivalent fix on url-shortener-core's original click-write.
 */
class LinkServiceFailureIsolationTest {

    @Test
    // AC41: redirect succeeds even though the click-write throws
    void redirectAndRecordClick_clickWriteThrows_redirectStillSucceeds() {
        ShortLinkRepository shortLinkRepository = mock(ShortLinkRepository.class);
        ClickEventRepository clickEventRepository = mock(ClickEventRepository.class);
        GeoLookupService geoLookupService = mock(GeoLookupService.class);

        ShortLinkEntity link = new ShortLinkEntity(
                "fail-test", "https://example.com/failure-isolation",
                Instant.now(), Instant.now().plusSeconds(86400));
        // id is @GeneratedValue -- null until persisted; ClickEventEntity requires a
        // non-null shortLinkId, so a bare `new` needs this set manually for the test.
        ReflectionTestUtils.setField(link, "id", 1L);
        when(shortLinkRepository.findByCode("fail-test")).thenReturn(Optional.of(link));
        when(geoLookupService.lookupCountry(any())).thenReturn(null);
        when(clickEventRepository.saveAndFlush(any())).thenThrow(new RuntimeException("simulated DB failure"));

        LinkService service = new LinkService(shortLinkRepository, clickEventRepository,
                new CodeGenerator(), geoLookupService);

        // Must NOT throw -- this is the actual behavior AC41 requires.
        ShortLinkEntity result = service.redirectAndRecordClick("fail-test", null, "203.0.113.1");

        assertThat(result.getTargetUrl()).isEqualTo("https://example.com/failure-isolation");
    }

    @Test
    // AC42: normal path unchanged -- click IS still recorded (via saveAndFlush) when nothing fails
    void redirectAndRecordClick_healthyPath_stillRecordsClickSynchronously() {
        ShortLinkRepository shortLinkRepository = mock(ShortLinkRepository.class);
        ClickEventRepository clickEventRepository = mock(ClickEventRepository.class);
        GeoLookupService geoLookupService = mock(GeoLookupService.class);

        ShortLinkEntity link = new ShortLinkEntity(
                "healthy", "https://example.com/healthy",
                Instant.now(), Instant.now().plusSeconds(86400));
        ReflectionTestUtils.setField(link, "id", 2L);
        when(shortLinkRepository.findByCode("healthy")).thenReturn(Optional.of(link));
        when(geoLookupService.lookupCountry(any())).thenReturn("US");

        LinkService service = new LinkService(shortLinkRepository, clickEventRepository,
                new CodeGenerator(), geoLookupService);

        service.redirectAndRecordClick("healthy", "https://ref.example", "203.0.113.2");

        // saveAndFlush, not save -- the fix must not regress to a deferred write
        // (technical-design.md's core correctness point).
        verify(clickEventRepository).saveAndFlush(any());
    }

    @Test
    // Regression guard: LinkUnavailableException (unknown/expired code) must still
    // propagate uncaught -- the try/catch must not swallow the earlier lookup checks.
    void redirectAndRecordClick_unknownCode_stillThrowsLinkUnavailable() {
        ShortLinkRepository shortLinkRepository = mock(ShortLinkRepository.class);
        ClickEventRepository clickEventRepository = mock(ClickEventRepository.class);
        GeoLookupService geoLookupService = mock(GeoLookupService.class);
        when(shortLinkRepository.findByCode("missing")).thenReturn(Optional.empty());

        LinkService service = new LinkService(shortLinkRepository, clickEventRepository,
                new CodeGenerator(), geoLookupService);

        assertThatThrownBy(() -> service.redirectAndRecordClick("missing", null, "203.0.113.3"))
                .isInstanceOf(LinkUnavailableException.class);
    }
}
