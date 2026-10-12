package com.dentalcare.tony.api.infrastructure.ratelimit;

import io.github.bucket4j.Bucket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory token-bucket cache, one bucket per (limit kind, client IP).
 *
 * <p>Single-instance only: buckets live in this JVM's heap. If the API is
 * ever scaled to multiple instances, replace this with a shared backend
 * (Redis + bucket4j-redis) keeping the same {@link #bucketFor} contract.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClientRateLimiter {

    static final String LOGIN_PATH_PREFIX = "/api/auth/";

    private final RateLimitProperties properties;
    private final ConcurrentHashMap<String, BucketEntry> buckets = new ConcurrentHashMap<>();

    /**
     * Returns (creating if needed) the bucket that applies to this request.
     * Login endpoints get the stricter {@code login} limit; everything else
     * gets the general {@code api} limit.
     */
    public Bucket bucketFor(String clientIp, String path) {

        boolean isLogin = path != null && path.startsWith(LOGIN_PATH_PREFIX);
        RateLimitProperties.Limit limit = isLogin ? properties.login() : properties.api();
        String key = (isLogin ? "login:" : "api:") + clientIp;

        return buckets.computeIfAbsent(key, k -> newBucket(limit)).bucket();
    }

    int bucketCount() {
        return buckets.size();
    }

    private BucketEntry newBucket(RateLimitProperties.Limit limit) {
        Bucket bucket = Bucket.builder()
                .addLimit(b -> b
                        .capacity(limit.capacity())
                        .refillGreedy(limit.refillTokens(), limit.refillPeriod()))
                .build();
        return new BucketEntry(bucket, limit.capacity());
    }

    /**
     * Evicts fully-refilled (idle) buckets so an attacker cannot grow the
     * map unboundedly with fake IPs, and idle clients do not pin memory.
     */
    @Scheduled(fixedDelay = 600_000, initialDelay = 600_000)
    public void evictIdleBuckets() {

        int before = buckets.size();
        buckets.entrySet().removeIf(e ->
                e.getValue().bucket().getAvailableTokens() >= e.getValue().capacity());

        int removed = before - buckets.size();
        if (removed > 0) {
            log.debug("Rate limiter evicted {} idle buckets ({} remaining)", removed, buckets.size());
        }
    }

    /** Bucket plus its configured capacity (no config introspection needed). */
    private record BucketEntry(Bucket bucket, long capacity) {
    }
}
