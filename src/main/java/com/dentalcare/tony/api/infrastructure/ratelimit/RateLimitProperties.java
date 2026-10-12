package com.dentalcare.tony.api.infrastructure.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuration for the per-client-IP rate limiter.
 *
 * <pre>
 * rate-limit.enabled=true
 * rate-limit.api.capacity=60
 * rate-limit.api.refill-tokens=60
 * rate-limit.api.refill-seconds=60
 * rate-limit.login.capacity=5
 * rate-limit.login.refill-tokens=5
 * rate-limit.login.refill-seconds=60
 * </pre>
 */
@ConfigurationProperties(prefix = "rate-limit")
public record RateLimitProperties(
        boolean enabled,
        Limit api,
        Limit login
) {

    /**
     * One token-bucket limit: {@code capacity} burst, refilled
     * {@code refillTokens} tokens every {@code refillSeconds}.
     */
    public record Limit(long capacity, long refillTokens, long refillSeconds) {

        public Duration refillPeriod() {
            return Duration.ofSeconds(refillSeconds);
        }
    }
}
