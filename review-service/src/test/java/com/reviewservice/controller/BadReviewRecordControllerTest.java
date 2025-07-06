package com.reviewservice.controller;

import com.reviewservice.service.BadReviewRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class BadReviewRecordControllerTest {

    private MockMvc mockMvc;
    private BadReviewRecordService badReviewRecordService;

    @BeforeEach
    void setup() {
        badReviewRecordService = Mockito.mock(BadReviewRecordService.class);
        BadReviewRecordController controller = new BadReviewRecordController(badReviewRecordService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getAllBadRecords_returnsOk() throws Exception {
        mockMvc.perform(get("/api/bad-review-records"))
                .andExpect(status().isOk());
    }
} 