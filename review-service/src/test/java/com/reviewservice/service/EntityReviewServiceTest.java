package com.reviewservice.service;

import com.reviewservice.repository.EntityReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class EntityReviewServiceTest {
    private EntityReviewRepository entityReviewRepository;
    private BadReviewRecordService badReviewRecordService;
    private EntityReviewService entityReviewService;

    @BeforeEach
    void setup() {
        entityReviewRepository = Mockito.mock(EntityReviewRepository.class);
        badReviewRecordService = Mockito.mock(BadReviewRecordService.class);
        entityReviewService = new EntityReviewService(entityReviewRepository, badReviewRecordService);
    }

    @Test
    void getAllReviewsBody_returnsNonNull() {
        Page page = new PageImpl(Collections.emptyList(), PageRequest.of(0, 1, Sort.by("reviewId")), 0);
        when(entityReviewRepository.findAll(any(Pageable.class))).thenReturn(page);
        Map<String, Object> result = entityReviewService.getAllReviewsBody(0, 1, "reviewId", "desc", null, null, null, null);
        assertNotNull(result);
    }
} 