package com.aisdlc.urlshortener;

import com.aisdlc.urlshortener.data.ShortLinkEntity;
import com.aisdlc.urlshortener.data.ShortLinkRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * risk-register.md R-1 mitigation: "Add an integration test that restarts the datasource
 * (or re-opens the file) and asserts a previously-created link still resolves."
 *
 * <p>Unlike the rest of the suite (which deliberately uses in-memory H2 for speed/isolation
 * -- see {@code application-test.yml}), this test exercises the actual FILE-mode
 * configuration ({@code jdbc:h2:file:...}) production uses (FR-5, Gate-0 "don't lose
 * data"), across two independent application-context lifecycles pointed at the same on-disk
 * database file -- proving durability across a real process restart, not merely that the
 * JDBC URL string contains the word "file".
 */
class H2FileModeDurabilityTest {

    @Test
    void linkSurvivesApplicationRestartAgainstTheSameH2File(@TempDir Path tempDir) {
        String dbPath = tempDir.resolve("durability-test").toAbsolutePath().toString().replace('\\', '/');
        String jdbcUrl = "jdbc:h2:file:" + dbPath + ";AUTO_SERVER=FALSE";

        String code;
        ConfigurableApplicationContext firstRun = startApplication(jdbcUrl);
        try {
            ShortLinkRepository repo = firstRun.getBean(ShortLinkRepository.class);
            Instant now = Instant.now();
            ShortLinkEntity saved = repo.saveAndFlush(
                    new ShortLinkEntity("durable1", "https://example.com/durable", now,
                            now.plus(30, ChronoUnit.DAYS)));
            code = saved.getCode();
        } finally {
            firstRun.close();
        }

        ConfigurableApplicationContext secondRun = startApplication(jdbcUrl);
        try {
            ShortLinkRepository repo = secondRun.getBean(ShortLinkRepository.class);
            Optional<ShortLinkEntity> reloaded = repo.findByCode(code);
            assertThat(reloaded).isPresent();
            assertThat(reloaded.get().getTargetUrl()).isEqualTo("https://example.com/durable");
        } finally {
            secondRun.close();
        }
    }

    /**
     * Properties are passed as command-line-style args to {@code .run(...)},
     * NOT via {@link SpringApplicationBuilder#properties}. That method
     * populates Spring Boot's "default properties" source, which is the
     * LOWEST-priority source — application.yml's own
     * {@code spring.datasource.url} (the real, shared, file-mode path)
     * silently wins over it. That bug was caught by actually running this
     * test twice: it passed once, then failed on every subsequent run with
     * a duplicate-key violation on 'durable1', because both "isolated" runs
     * were actually hitting the same shared ./data/urlshortener file, not
     * fresh @TempDir instances. Command-line args are one of the highest
     * -precedence Spring Boot property sources, above application.yml.
     */
    private ConfigurableApplicationContext startApplication(String jdbcUrl) {
        return new SpringApplicationBuilder(UrlShortenerApplication.class)
                .web(WebApplicationType.NONE)
                .run(
                        "--spring.datasource.url=" + jdbcUrl,
                        "--spring.datasource.driver-class-name=org.h2.Driver",
                        "--spring.jpa.hibernate.ddl-auto=none",
                        "--spring.sql.init.mode=always",
                        "--spring.h2.console.enabled=false",
                        "--urlshortener.geoip.database-path=./geoip/GeoLite2-Country.mmdb"
                );
    }
}
