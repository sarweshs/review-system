package com.reviewservice.controller;

import com.reviewservice.service.CacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CacheManagementControllerTest {

    private MockMvc mockMvc;
    private CacheService cacheService;

    @BeforeEach
    void setup() {
        cacheService = Mockito.mock(CacheService.class);
        CacheManagementController controller = new CacheManagementController(cacheService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void invalidateAllCaches_returnsOk() throws Exception {
        mockMvc.perform(post("/api/admin/cache/invalidate-all"))
                .andExpect(status().isOk());
    }
} 