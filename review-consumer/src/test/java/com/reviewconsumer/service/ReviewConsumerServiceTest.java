package com.reviewconsumer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reviewcore.dto.BadReviewMessage;
import com.reviewcore.model.BadReviewRecord;
import com.reviewconsumer.repository.BadReviewRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewConsumerServiceTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private MetricsService metricsService;

    @Mock
    private BadReviewRecordRepository badReviewRecordRepository;

    @Mock
    private ReviewProcessingService reviewProcessingService;

    private ReviewConsumerService reviewConsumerService;

    @BeforeEach
    void setUp() {
        reviewConsumerService = new ReviewConsumerService(objectMapper, metricsService, badReviewRecordRepository, reviewProcessingService);
    }

    @Test
    void shouldExtractSpecificValidationReasonFromBadReviewMessage() throws Exception {
        // Given
        BadReviewMessage badReviewMessage = new BadReviewMessage(
            "{\"hotel_id\": null, \"platform\": \"booking.com\"}",
            "booking.com",
            "HOTEL_ID_NULL"
        );

        BadReviewRecord savedRecord = new BadReviewRecord();
        savedRecord.setId(1L);
        savedRecord.setJsonData("test");
        savedRecord.setPlatform("booking.com");
        savedRecord.setReason("HOTEL_ID_NULL");

        when(objectMapper.writeValueAsString(any(BadReviewMessage.class)))
            .thenReturn("{\"jsonData\":\"test\",\"platform\":\"booking.com\",\"reason\":\"HOTEL_ID_NULL\"}");
        when(badReviewRecordRepository.save(any(BadReviewRecord.class)))
            .thenReturn(savedRecord);

        // When
        reviewConsumerService.processBadReview(badReviewMessage);

        // Then
        ArgumentCaptor<BadReviewRecord> recordCaptor = ArgumentCaptor.forClass(BadReviewRecord.class);
        verify(badReviewRecordRepository).save(recordCaptor.capture());

        BadReviewRecord capturedRecord = recordCaptor.getValue();
        assertEquals("HOTEL_ID_NULL", capturedRecord.getReason());
        assertEquals("booking.com", capturedRecord.getPlatform());
    }

    @Test
    void shouldHandleDifferentValidationReasons() throws Exception {
        // Given
        BadReviewMessage badReviewMessage = new BadReviewMessage(
            "{\"rating\": \"invalid\", \"platform\": \"tripadvisor.com\"}",
            "tripadvisor.com",
            "INVALID_RATING_FORMAT"
        );

        BadReviewRecord savedRecord = new BadReviewRecord();
        savedRecord.setId(2L);
        savedRecord.setJsonData("test");
        savedRecord.setPlatform("tripadvisor.com");
        savedRecord.setReason("INVALID_RATING_FORMAT");

        when(objectMapper.writeValueAsString(any(BadReviewMessage.class)))
            .thenReturn("{\"jsonData\":\"test\",\"platform\":\"tripadvisor.com\",\"reason\":\"INVALID_RATING_FORMAT\"}");
        when(badReviewRecordRepository.save(any(BadReviewRecord.class)))
            .thenReturn(savedRecord);

        // When
        reviewConsumerService.processBadReview(badReviewMessage);

        // Then
        ArgumentCaptor<BadReviewRecord> recordCaptor = ArgumentCaptor.forClass(BadReviewRecord.class);
        verify(badReviewRecordRepository).save(recordCaptor.capture());

        BadReviewRecord capturedRecord = recordCaptor.getValue();
        assertEquals("INVALID_RATING_FORMAT", capturedRecord.getReason());
        assertEquals("tripadvisor.com", capturedRecord.getPlatform());
    }
} 