package com.reviewservice.controller;

import com.reviewservice.service.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/admin/cache")
@CrossOrigin(origins = "*", allowedHeaders = "*", allowCredentials = "false")
@RequiredArgsConstructor
public class CacheManagementController {
    
    private final CacheService cacheService;
    
    /**
     * Invalidate all caches
     */
    @PostMapping("/invalidate-all")
    public ResponseEntity<Map<String, Object>> invalidateAllCaches() {
        log.info("Admin requested to invalidate all caches");
        
        try {
            cacheService.invalidateAllCaches();
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "All caches invalidated successfully");
            response.put("timestamp", java.time.Instant.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to invalidate all caches", e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to invalidate caches: " + e.getMessage());
            error.put("timestamp", java.time.Instant.now());
            
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * Invalidate specific cache
     */
    @PostMapping("/invalidate/{cacheName}")
    public ResponseEntity<Map<String, Object>> invalidateCache(@PathVariable String cacheName) {
        log.info("Admin requested to invalidate cache: {}", cacheName);
        
        try {
            if (!cacheService.cacheExists(cacheName)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Cache not found: " + cacheName);
                error.put("timestamp", java.time.Instant.now());
                return ResponseEntity.badRequest().body(error);
            }
            
            cacheService.invalidateCache(cacheName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Cache '" + cacheName + "' invalidated successfully");
            response.put("cacheName", cacheName);
            response.put("timestamp", java.time.Instant.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to invalidate cache: {}", cacheName, e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to invalidate cache: " + e.getMessage());
            error.put("cacheName", cacheName);
            error.put("timestamp", java.time.Instant.now());
            
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * Invalidate reviews cache
     */
    @PostMapping("/invalidate-reviews")
    public ResponseEntity<Map<String, Object>> invalidateReviewsCache() {
        log.info("Admin requested to invalidate reviews cache");
        
        try {
            cacheService.invalidateReviewsCache();
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Reviews cache invalidated successfully");
            response.put("timestamp", java.time.Instant.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to invalidate reviews cache", e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to invalidate reviews cache: " + e.getMessage());
            error.put("timestamp", java.time.Instant.now());
            
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * Invalidate bad reviews cache
     */
    @PostMapping("/invalidate-bad-reviews")
    public ResponseEntity<Map<String, Object>> invalidateBadReviewsCache() {
        log.info("Admin requested to invalidate bad reviews cache");
        
        try {
            cacheService.invalidateBadReviewsCache();
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Bad reviews cache invalidated successfully");
            response.put("timestamp", java.time.Instant.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to invalidate bad reviews cache", e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to invalidate bad reviews cache: " + e.getMessage());
            error.put("timestamp", java.time.Instant.now());
            
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * Invalidate statistics cache
     */
    @PostMapping("/invalidate-stats")
    public ResponseEntity<Map<String, Object>> invalidateStatsCache() {
        log.info("Admin requested to invalidate statistics cache");
        
        try {
            cacheService.invalidateStatsCache();
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Statistics cache invalidated successfully");
            response.put("timestamp", java.time.Instant.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to invalidate statistics cache", e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to invalidate statistics cache: " + e.getMessage());
            error.put("timestamp", java.time.Instant.now());
            
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * Get cache information
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getCacheInfo() {
        log.info("Admin requested cache information");
        
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("cacheNames", cacheService.getCacheNames());
            response.put("cacheStatistics", cacheService.getCacheStatistics());
            response.put("timestamp", java.time.Instant.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get cache information", e);
            
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to get cache information: " + e.getMessage());
            error.put("timestamp", java.time.Instant.now());
            
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * Health check for cache management
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "cache-management");
        health.put("timestamp", java.time.Instant.now());
        health.put("availableCaches", cacheService.getCacheNames().size());
        
        return ResponseEntity.ok(health);
    }
} 