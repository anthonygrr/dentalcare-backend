package com.dentalcare.tony.api.infrastructure.ratelimit;

import com.dentalcare.tony.api.presentation.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * Rejects requests over the per-client-IP token bucket with 429 Too Many
 * Requests. Registered before the Spring Security chain (see
 * {@link RateLimitConfig}) so every request — authenticated or not —
 * consumes a token.
 */
@Slf4j
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    /** Servlet 6.1 defines SC_TOO_MANY_REQUESTS; the compile classpath may be older. */
    static final int TOO_MANY_REQUESTS = 429;

    private final ClientRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;
    private final RateLimitProperties properties;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        Bucket bucket = rateLimiter.bucketFor(clientIp(request), request.getRequestURI());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.setHeader(
                    "X-Rate-Limit-Remaining",
                    String.valueOf(probe.getRemainingTokens())
            );
            filterChain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds = TimeUnit.NANOSECONDS
                .toSeconds(probe.getNanosToWaitForRefill());

        response.setStatus(TOO_MANY_REQUESTS);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));

        ErrorResponse body = ErrorResponse.builder()
                .message("Too many requests, retry in " + retryAfterSeconds + "s")
                .status(TOO_MANY_REQUESTS)
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .errors(null)
                .build();

        objectMapper.writeValue(response.getOutputStream(), body);
        log.debug("Rate limit exceeded for {} on {}",
                clientIp(request), request.getRequestURI());
    }

    /**
     * First hop of {@code X-Forwarded-For} when present, else the socket
     * address. Only trust the header behind a proxy that overwrites it —
     * otherwise a client can spoof its way into fresh buckets.
     */
    static String clientIp(HttpServletRequest request) {

        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return request.getRemoteAddr();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // kill-switch (rate-limit.enabled=false) + CORS preflight never
        // reaches the controllers — do not charge for either
        return !properties.enabled()
                || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }
}
