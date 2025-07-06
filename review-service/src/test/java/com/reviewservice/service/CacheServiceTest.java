package com.reviewservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CacheServiceTest {
    private CacheManager cacheManager;
    private CacheService cacheService;
    private Cache cache;

    @BeforeEach
    void setup() {
        cacheManager = Mockito.mock(CacheManager.class);
        cacheService = new CacheService(cacheManager);
        cache = Mockito.mock(Cache.class);
    }

    @Test
    void invalidateAllCaches_callsClearOnAllCaches() {
        Set<String> names = new HashSet<>();
        names.add("foo");
        when(cacheManager.getCacheNames()).thenReturn(names);
        when(cacheManager.getCache("foo")).thenReturn(cache);
        cacheService.invalidateAllCaches();
        verify(cache, times(1)).clear();
    }

    @Test
    void invalidateCache_clearsIfExists() {
        when(cacheManager.getCache("bar")).thenReturn(cache);
        cacheService.invalidateCache("bar");
        verify(cache, times(1)).clear();
    }

    @Test
    void invalidateCache_doesNothingIfNotExists() {
        when(cacheManager.getCache("baz")).thenReturn(null);
        cacheService.invalidateCache("baz");
        // No exception, nothing to verify
    }

    @Test
    void cacheExists_returnsTrueIfExists() {
        when(cacheManager.getCache("foo")).thenReturn(cache);
        assertTrue(cacheService.cacheExists("foo"));
    }

    @Test
    void cacheExists_returnsFalseIfNotExists() {
        when(cacheManager.getCache("bar")).thenReturn(null);
        assertFalse(cacheService.cacheExists("bar"));
    }

    @Test
    void getCacheNames_returnsNames() {
        Set<String> names = new HashSet<>();
        names.add("foo");
        when(cacheManager.getCacheNames()).thenReturn(names);
        assertEquals(names, cacheService.getCacheNames());
    }
} 