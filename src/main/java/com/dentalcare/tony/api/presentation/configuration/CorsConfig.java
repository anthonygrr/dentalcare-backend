package com.dentalcare.tony.api.presentation.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // Applies to all endpoints
                .allowedOrigins("http://localhost:8080") // Allowed Frontend
                .allowedOrigins("http://localhost:5173") // Allowed Frontend
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Allowed Methods
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
