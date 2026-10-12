package com.dentalcare.tony.api.infrastructure.ratelimit;

import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClientRateLimiterTest {

    private static final RateLimitProperties.Limit API =
            new RateLimitProperties.Limit(3, 3, 60);
    private static final RateLimitProperties.Limit LOGIN =
            new RateLimitProperties.Limit(2, 2, 60);

    private final ClientRateLimiter limiter =
            new ClientRateLimiter(new RateLimitProperties(true, API, LOGIN));

    @Test
    void reusesTheBucketForTheSameClientAcrossGeneralEndpoints() {
        Bucket patients = limiter.bucketFor("1.2.3.4", "/patients");
        Bucket doctors = limiter.bucketFor("1.2.3.4", "/doctors");

        assertSame(patients, doctors,
                "general endpoints must share one bucket per client IP");
    }

    @Test
    void usesASeparateStricterBucketForLoginEndpoints() {
        Bucket general = limiter.bucketFor("1.2.3.4", "/patients");
        Bucket login = limiter.bucketFor("1.2.3.4", "/api/auth/login");

        assertNotSame(general, login);
        assertTrue(login.tryConsume(2), "login bucket fills its own capacity");
        assertFalse(login.tryConsume(1), "login bucket blocks past its capacity");
    }

    @Test
    void separatesClientsByIp() {
        Bucket a = limiter.bucketFor("10.0.0.1", "/patients");
        Bucket b = limiter.bucketFor("10.0.0.2", "/patients");

        assertNotSame(a, b);
    }

    @Test
    void evictsFullyRefilledIdleBuckets() {
        limiter.bucketFor("9.9.9.9", "/patients");
        assertEquals(1, limiter.bucketCount());

        limiter.evictIdleBuckets();

        assertEquals(0, limiter.bucketCount(), "idle buckets must not pin memory");
    }

    @Test
    void keepsBusyBucketsDuringEviction() {
        Bucket busy = limiter.bucketFor("8.8.8.8", "/patients");
        assertTrue(busy.tryConsume(3), "drain the bucket");

        limiter.evictIdleBuckets();

        assertEquals(1, limiter.bucketCount(), "an exhausted bucket is not idle");
    }
}
