package com.aisdlc.urlshortener.api;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers {@link RateLimitInterceptor} against the redirect path only ({@code /{code}} --
 * a single root-level path segment), explicitly excluding {@code /api/**} and {@code
 * /actuator/**} -- feature-spec.md Section 6: "create, not stats" are not the abuse surface
 * named by FR-9. Also registers {@link BatchRateLimitInterceptor} against {@code
 * /api/v1/links/batch} only -- a second, separate registration (technical-design.md Section
 * 4 item 2), not a modification of the rule above; the batch path already falls inside the
 * existing {@code excludePathPatterns("/api/**", ...)} exclusion for {@link
 * RateLimitInterceptor}, which is left untouched.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;
    private final BatchRateLimitInterceptor batchRateLimitInterceptor;

    public WebConfig(RateLimitInterceptor rateLimitInterceptor,
                      BatchRateLimitInterceptor batchRateLimitInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
        this.batchRateLimitInterceptor = batchRateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/*")
                .excludePathPatterns("/api/**", "/actuator/**");
        registry.addInterceptor(batchRateLimitInterceptor)
                .addPathPatterns("/api/v1/links/batch");
    }
}
