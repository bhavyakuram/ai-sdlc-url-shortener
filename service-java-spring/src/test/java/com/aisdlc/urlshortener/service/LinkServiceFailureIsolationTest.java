package com.aisdlc.urlshortener.service;

import com.aisdlc.urlshortener.data.ClickEventRepository;
import com.aisdlc.urlshortener.data.ShortLinkEntity;
import com.aisdlc.urlshortener.data.ShortLinkRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Plain-Mockito unit test (no Spring context needed) that forces the
 * click-write to throw, proving AC-15/AC-16 from
 * url-shortener-analytics-reliability directly, rather than trusting
 * the try/catch by inspection alone.
 */
class LinkServiceFailureIsolationTest {

    @Test
    // AC-15: redirect succeeds even though the click-write throws
    void resolveAndRecordClick_clickWriteThrows_redirectStillSucceeds() {
        ShortLinkRepository shortLinkRepository = mock(ShortLinkRepository.class);
        ClickEventRepository clickEventRepository = mock(ClickEventRepository.class);
        CodeGenerator codeGenerator = new CodeGenerator();

        ShortLinkEntity link = new ShortLinkEntity(
                "fail-test", "https://example.com/failure-isolation",
                Instant.now(), Instant.now().plusSeconds(86400));
        when(shortLinkRepository.findByShortCode("fail-test")).thenReturn(Optional.of(link));
        when(clickEventRepository.save(any())).thenThrow(new RuntimeException("simulated DB failure"));

        LinkService service = new LinkService(shortLinkRepository, clickEventRepository, codeGenerator, 90);

        // Must NOT throw — this is the actual behavior AC-15 requires.
        String target = service.resolveAndRecordClick("fail-test", null);

        assertThat(target).isEqualTo("https://example.com/failure-isolation");
    }
}
