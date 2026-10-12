package com.dentalcare.tony.api.infrastructure.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Wires the rate-limit filter ahead of the Spring Security filter chain
 * (order {@code -101}) so unauthenticated floods are throttled before
 * JWT parsing, and enables the idle-bucket eviction schedule.
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitConfig {

    @Bean
    public RateLimitFilter rateLimitFilter(
            ClientRateLimiter rateLimiter,
            ObjectMapper objectMapper,
            RateLimitProperties properties
    ) {
        return new RateLimitFilter(rateLimiter, objectMapper, properties);
    }

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(
            RateLimitFilter filter
    ) {
        FilterRegistrationBean<RateLimitFilter> registration =
                new FilterRegistrationBean<>(filter);

        registration.setOrder(SecurityProperties.DEFAULT_FILTER_ORDER - 1);
        registration.addUrlPatterns("/*");

        return registration;
    }
}
