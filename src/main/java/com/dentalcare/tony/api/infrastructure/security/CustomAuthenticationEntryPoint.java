package com.dentalcare.tony.api.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Writes the 401 JSON body for unauthenticated requests.
 *
 * <p>Uses the Spring-managed {@code ObjectMapper}: a raw {@code new
 * ObjectMapper()} lacks the jsr310 module and cannot serialize the
 * {@code LocalDateTime} timestamp, which used to crash this handler
 * mid-response (empty body + broken connection).</p>
 */
@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint
        implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        response.setContentType(
                MediaType.APPLICATION_JSON_VALUE
        );

        Map<String, Object> body = new LinkedHashMap<>();

        body.put("timestamp", LocalDateTime.now());
        body.put("status", 401);
        body.put("error", "Unauthorized");
        body.put("message", "Authentication required");
        body.put("path", request.getRequestURI());

        objectMapper
                .writeValue(response.getOutputStream(), body);
    }
}
