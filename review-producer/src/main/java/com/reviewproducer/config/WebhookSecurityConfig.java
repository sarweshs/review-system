package com.reviewproducer.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Slf4j
@Configuration
@EnableWebSecurity
public class WebhookSecurityConfig {
    
    @Value("${webhook.api.key:default-webhook-key}")
    private String webhookApiKey;
    
    @Value("${webhook.api.key.header:X-API-Key}")
    private String apiKeyHeader;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                // Allow health check and metrics endpoints without authentication
                .requestMatchers("/api/producer/health", "/api/producer/metrics").permitAll()
                // Require API key for storage event endpoints
                .requestMatchers("/api/producer/storage/**").authenticated()
                // Allow other endpoints (for backward compatibility)
                .requestMatchers("/api/producer/**").permitAll()
                .anyRequest().permitAll()
            )
            .addFilterBefore(apiKeyAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public ApiKeyAuthenticationFilter apiKeyAuthenticationFilter() {
        return new ApiKeyAuthenticationFilter(webhookApiKey, apiKeyHeader);
    }
} 