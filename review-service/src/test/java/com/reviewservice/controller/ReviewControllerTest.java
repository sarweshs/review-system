package com.reviewservice.controller;

import com.reviewservice.service.EntityReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ReviewControllerTest {

    private MockMvc mockMvc;
    private EntityReviewService entityReviewService;

    @BeforeEach
    void setup() {
        entityReviewService = Mockito.mock(EntityReviewService.class);
        ReviewController controller = new ReviewController(entityReviewService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getAllReviews_returnsOk() throws Exception {
        mockMvc.perform(get("/api/reviews")
                .param("page", "0")
                .param("size", "1"))
                .andExpect(status().isOk());
    }
} 