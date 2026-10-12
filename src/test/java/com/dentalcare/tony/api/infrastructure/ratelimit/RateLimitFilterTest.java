package com.dentalcare.tony.api.infrastructure.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitFilterTest {

    private static final RateLimitProperties.Limit API =
            new RateLimitProperties.Limit(3, 3, 60);
    private static final RateLimitProperties.Limit LOGIN =
            new RateLimitProperties.Limit(2, 2, 60);

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        RateLimitProperties properties = new RateLimitProperties(true, API, LOGIN);
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        filter = new RateLimitFilter(new ClientRateLimiter(properties), mapper, properties);
    }

    @Test
    void disabledKillSwitchPassesEverythingWithoutConsumingTokens() throws Exception {
        RateLimitProperties disabled = new RateLimitProperties(false, API, LOGIN);
        RateLimitFilter offFilter = new RateLimitFilter(
                new ClientRateLimiter(disabled),
                new ObjectMapper().registerModule(new JavaTimeModule()),
                disabled);

        // more requests than capacity would allow — none may be throttled
        for (int i = 0; i < API.capacity() + 2; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();
            offFilter.doFilter(request("GET", "/patients", "10.0.0.9"), response, chain);

            assertNotNull(chain.getRequest(), "disabled filter must always pass");
            assertNull(response.getHeader("X-Rate-Limit-Remaining"),
                    "disabled filter must not charge tokens");
        }
    }

    @Test
    void passesRequestsUnderTheLimitAndReportsRemainingTokens() throws Exception {
        MockHttpServletRequest request = request("GET", "/patients", "10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNotNull(chain.getRequest(), "request must reach the filter chain");
        assertEquals(String.valueOf(API.capacity() - 1),
                response.getHeader("X-Rate-Limit-Remaining"));
    }

    @Test
    void rejectsRequestsOverTheLimitWith429AndRetryAfter() throws Exception {
        for (int i = 0; i < API.capacity(); i++) {
            burn("GET", "/patients", "10.0.0.2");
        }

        MockHttpServletRequest request = request("GET", "/patients", "10.0.0.2");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(429, response.getStatus());
        assertNull(chain.getRequest(), "blocked request must not reach the chain");
        assertNotNull(response.getHeader("Retry-After"));
        assertTrue(response.getContentAsString().contains("Too many requests"));
        assertTrue(response.getContentAsString().contains("\"status\":429"));
    }

    @Test
    void appliesTheStricterLoginBucketToAuthEndpoints() throws Exception {
        for (int i = 0; i < LOGIN.capacity(); i++) {
            burn("POST", "/api/auth/login", "10.0.0.3");
        }

        MockHttpServletRequest blocked = request("POST", "/api/auth/login", "10.0.0.3");
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        MockFilterChain blockedChain = new MockFilterChain();
        filter.doFilter(blocked, blockedResponse, blockedChain);

        assertEquals(429, blockedResponse.getStatus(),
                "login attempts over capacity must be throttled");

        MockHttpServletRequest general = request("GET", "/patients", "10.0.0.3");
        MockHttpServletResponse generalResponse = new MockHttpServletResponse();
        filter.doFilter(general, generalResponse, new MockFilterChain());

        assertEquals(200, generalResponse.getStatus(),
                "the general bucket is independent from the login bucket");
    }

    @Test
    void keysBucketsByTheFirstForwardedIp() throws Exception {
        for (int i = 0; i < API.capacity(); i++) {
            burnForwarded("1.1.1.1, 2.2.2.2");
        }

        MockHttpServletRequest spoofedSameIp = forwardedRequest("1.1.1.1, 9.9.9.9");
        MockHttpServletResponse sameIpResponse = new MockHttpServletResponse();
        filter.doFilter(spoofedSameIp, sameIpResponse, new MockFilterChain());
        assertEquals(429, sameIpResponse.getStatus(), "same first hop shares the bucket");

        MockHttpServletRequest otherClient = forwardedRequest("3.3.3.3, 2.2.2.2");
        MockHttpServletResponse otherResponse = new MockHttpServletResponse();
        filter.doFilter(otherClient, otherResponse, new MockFilterChain());
        assertEquals(200, otherResponse.getStatus(), "different first hop gets a fresh bucket");
    }

    @Test
    void skipsCorsPreflightWithoutConsumingTokens() throws Exception {
        MockHttpServletRequest options = request("OPTIONS", "/patients", "10.0.0.4");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(options, response, chain);

        assertNotNull(chain.getRequest());
        assertNull(response.getHeader("X-Rate-Limit-Remaining"),
                "preflight must not consume tokens");
    }

    private void burn(String method, String path, String ip) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request(method, path, ip), response, new MockFilterChain());
    }

    private void burnForwarded(String forwardedFor) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(forwardedRequest(forwardedFor), response, new MockFilterChain());
    }

    private MockHttpServletRequest request(String method, String path, String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRemoteAddr(ip);
        return request;
    }

    private MockHttpServletRequest forwardedRequest(String forwardedFor) {
        MockHttpServletRequest request = request("GET", "/patients", "127.0.0.1");
        request.addHeader("X-Forwarded-For", forwardedFor);
        return request;
    }
}
