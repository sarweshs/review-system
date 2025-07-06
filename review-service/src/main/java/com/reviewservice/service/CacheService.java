package com.reviewservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {
    
    private final CacheManager cacheManager;
    
    /**
     * Invalidate all caches
     */
    public void invalidateAllCaches() {
        log.info("Invalidating all caches");
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                log.info("Cleared cache: {}", cacheName);
            }
        });
    }
    
    /**
     * Invalidate specific cache by name
     */
    public void invalidateCache(String cacheName) {
        log.info("Invalidating cache: {}", cacheName);
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            log.info("Successfully cleared cache: {}", cacheName);
        } else {
            log.warn("Cache not found: {}", cacheName);
        }
    }
    
    /**
     * Invalidate reviews cache
     */
    public void invalidateReviewsCache() {
        invalidateCache("reviews");
    }
    
    /**
     * Invalidate bad reviews cache
     */
    public void invalidateBadReviewsCache() {
        invalidateCache("bad-reviews");
    }
    
    /**
     * Invalidate statistics cache
     */
    public void invalidateStatsCache() {
        invalidateCache("review-stats");
    }
    
    /**
     * Invalidate summary cache
     */
    public void invalidateSummaryCache() {
        invalidateCache("review-summary");
    }
    
    /**
     * Get cache statistics
     */
    public Map<String, Object> getCacheStatistics() {
        Map<String, Object> stats = new java.util.HashMap<>();
        
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                // Note: Spring Cache doesn't provide direct statistics
                // This is a placeholder for cache info
                Map<String, Object> cacheInfo = new java.util.HashMap<>();
                cacheInfo.put("name", cacheName);
                cacheInfo.put("nativeCache", cache.getNativeCache().getClass().getSimpleName());
                stats.put(cacheName, cacheInfo);
            }
        });
        
        return stats;
    }
    
    /**
     * Get all cache names
     */
    public Set<String> getCacheNames() {
        return new java.util.HashSet<>(cacheManager.getCacheNames());
    }
    
    /**
     * Check if cache exists
     */
    public boolean cacheExists(String cacheName) {
        return cacheManager.getCache(cacheName) != null;
    }
} 