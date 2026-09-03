package com.aisdlc.urlshortener.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for GeoLookupService's fail-soft contract (R-7, AC20) in isolation from the
 * full Spring context. No real {@code .mmdb} file is provisioned in this build environment
 * by design (R-7) -- these tests assert the constructor never throws and lookups degrade to
 * null rather than exercising real MaxMind data (which would require a licensed download,
 * out of scope here).
 */
class GeoLookupServiceTest {

    @Test
    void constructorDoesNotThrowWhenDatabaseFileIsMissing() {
        GeoLookupService service = new GeoLookupService("./nonexistent-path/does-not-exist.mmdb");
        assertThat(service).isNotNull();
    }

    @Test
    void lookupReturnsNullWhenDatabaseIsUnavailable() {
        GeoLookupService service = new GeoLookupService("./nonexistent-path/does-not-exist.mmdb");
        assertThat(service.lookupCountry("8.8.8.8")).isNull();
    }

    @Test
    void lookupReturnsNullForBlankOrNullAddress() {
        GeoLookupService service = new GeoLookupService("./nonexistent-path/does-not-exist.mmdb");
        assertThat(service.lookupCountry(null)).isNull();
        assertThat(service.lookupCountry("")).isNull();
        assertThat(service.lookupCountry("   ")).isNull();
    }

    @Test
    void constructorDoesNotThrowForABlankPath() {
        GeoLookupService service = new GeoLookupService("");
        assertThat(service.lookupCountry("8.8.8.8")).isNull();
    }
}
