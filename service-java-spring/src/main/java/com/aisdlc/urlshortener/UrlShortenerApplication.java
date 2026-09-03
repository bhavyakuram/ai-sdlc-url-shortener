package com.aisdlc.urlshortener;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the URL Shortener service (java-spring stack, greenfield role).
 *
 * <p>Layering: {@code api -> service -> data} only, per {@code rules/architecture.md}
 * Dependency Direction. See {@code technical-design.md} for the component decomposition.
 */
@SpringBootApplication
public class UrlShortenerApplication {

    public static void main(String[] args) {
        SpringApplication.run(UrlShortenerApplication.class, args);
    }
}
