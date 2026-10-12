package com.dentalcare.tony.api.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Writes the 403 JSON body for authenticated-but-forbidden requests.
 *
 * <p>Uses the Spring-managed {@code ObjectMapper}: a raw {@code new
 * ObjectMapper()} lacks the jsr310 module and cannot serialize the
 * {@code LocalDateTime} timestamp, which used to crash this handler
 * mid-response (empty body + broken connection).</p>
 */
@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler
        implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException, ServletException {

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        response.setContentType(
                MediaType.APPLICATION_JSON_VALUE
        );

        Map<String, Object> body = new LinkedHashMap<>();

        body.put("timestamp", LocalDateTime.now());
        body.put("status", 403);
        body.put("error", "Forbidden");
        body.put("message", "Access denied");
        body.put("path", request.getRequestURI());

        objectMapper
                .writeValue(response.getOutputStream(), body);
    }
}
