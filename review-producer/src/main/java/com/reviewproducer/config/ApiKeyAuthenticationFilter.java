package com.reviewproducer.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {
    
    private final String expectedApiKey;
    private final String apiKeyHeader;
    
    public ApiKeyAuthenticationFilter(String expectedApiKey, String apiKeyHeader) {
        this.expectedApiKey = expectedApiKey;
        this.apiKeyHeader = apiKeyHeader;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String requestPath = request.getRequestURI();
        
        // Only apply authentication to storage event endpoints
        if (requestPath.startsWith("/api/producer/storage/")) {
            String providedApiKey = request.getHeader(apiKeyHeader);
            
            if (providedApiKey == null || providedApiKey.trim().isEmpty()) {
                log.warn("Missing API key for request: {}", requestPath);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\": \"Missing API key\"}");
                return;
            }
            
            if (!expectedApiKey.equals(providedApiKey)) {
                log.warn("Invalid API key for request: {}", requestPath);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\": \"Invalid API key\"}");
                return;
            }
            
            // Set authentication context
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "webhook-client",
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_WEBHOOK"))
            );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("API key authentication successful for request: {}", requestPath);
        }
        
        filterChain.doFilter(request, response);
    }
} 