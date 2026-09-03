package com.aisdlc.urlshortener.service;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CountryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;

/**
 * Country-level geo-IP lookup backed by an offline MaxMind GeoLite2-Country {@code .mmdb}
 * file (feasibility-report.md Part 2 friction point #3 dependency pick).
 *
 * <p><b>Fails soft, always.</b> Since late 2019 MaxMind requires a free account, an accepted
 * EULA, and a license key (which itself expires every 90 days) to download the actual
 * {@code GeoLite2-Country.mmdb} database -- the file is not a Maven artifact and is not
 * bundled by the {@code geoip2} reader library (risk-register.md R-7). This build
 * environment intentionally has no such file/license provisioned. Both the constructor
 * (missing/unreadable file at startup) and {@link #lookupCountry(String)} (malformed
 * address, corrupt DB, any other runtime failure) therefore catch broadly and degrade to
 * "no country" rather than let a geo-IP problem fail application startup or fail a redirect
 * request -- see {@code rules/coding-standards.md} No Silent Catches (logged with context,
 * not swallowed silently) and AC20.
 */
@Service
public class GeoLookupService {

    private static final Logger log = LoggerFactory.getLogger(GeoLookupService.class);

    private final DatabaseReader databaseReader;

    public GeoLookupService(@Value("${urlshortener.geoip.database-path}") String databasePath) {
        this.databaseReader = openReaderOrNull(databasePath);
    }

    private static DatabaseReader openReaderOrNull(String databasePath) {
        try {
            File dbFile = new File(databasePath);
            return new DatabaseReader.Builder(dbFile).build();
        } catch (IOException | RuntimeException ex) {
            // Provisioning the real GeoLite2-Country.mmdb requires a MaxMind account +
            // license key (risk-register.md R-7) -- not available in this build
            // environment. Fail soft: log once at startup, run with geo lookups disabled.
            log.warn("GeoIP database unavailable at '{}' (R-7: requires a MaxMind account "
                    + "and license key, not bundled/resolvable via Maven) -- country lookups "
                    + "will return null/\"unknown\" for every click until a real .mmdb file "
                    + "is provisioned.", databasePath, ex);
            return null;
        }
    }

    /**
     * Returns the ISO country code for {@code ipAddress}, or {@code null} if the database
     * is unavailable, the address can't be resolved, or the lookup otherwise fails. Never
     * throws. Callers (LinkService) treat a null result as the stats endpoint's
     * {@code "unknown"} bucket (feature-spec.md 3.3), not as an error.
     */
    public String lookupCountry(String ipAddress) {
        if (databaseReader == null || ipAddress == null || ipAddress.isBlank()) {
            return null;
        }
        try {
            InetAddress address = InetAddress.getByName(ipAddress);
            CountryResponse response = databaseReader.country(address);
            return response.getCountry() == null ? null : response.getCountry().getIsoCode();
        } catch (IOException | GeoIp2Exception | RuntimeException ex) {
            log.warn("Geo-IP lookup failed for address '{}' -- recording this click without "
                    + "a country.", ipAddress, ex);
            return null;
        }
    }
}
