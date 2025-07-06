package com.reviewservice.service;

import com.reviewservice.repository.BadReviewRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

public class BadReviewRecordServiceTest {
    private BadReviewRecordRepository badReviewRecordRepository;
    private BadReviewRecordService badReviewRecordService;

    @BeforeEach
    void setup() {
        badReviewRecordRepository = Mockito.mock(BadReviewRecordRepository.class);
        badReviewRecordService = new BadReviewRecordService(badReviewRecordRepository);
    }

    @Test
    void getAllBadRecords_returnsNonNull() {
        when(badReviewRecordRepository.findAll()).thenReturn(Collections.emptyList());
        List result = badReviewRecordService.getAllBadRecords();
        assertNotNull(result);
    }
} 