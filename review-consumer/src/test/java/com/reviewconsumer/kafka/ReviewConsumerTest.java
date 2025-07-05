package com.reviewconsumer.kafka;

import com.reviewconsumer.service.ReviewProcessingService;
import com.reviewcore.dto.ReviewMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewConsumerTest {

    @Mock
    private ReviewProcessingService reviewProcessingService;

    @Mock
    private Acknowledgment acknowledgment;

    private ReviewConsumer reviewConsumer;

    @BeforeEach
    void setUp() {
        reviewConsumer = new ReviewConsumer(reviewProcessingService);
    }

    @Test
    void shouldProcessReviewMessageSuccessfully() {
        // Given
        ReviewMessage reviewMessage = createSampleReviewMessage();
        String topic = "reviews";
        int partition = 0;
        long offset = 1L;
        long timestamp = System.currentTimeMillis();
        org.apache.kafka.clients.consumer.ConsumerRecord<String, com.reviewcore.dto.ReviewMessage> record = null;
        
        // When
        reviewConsumer.consumeReviewMessage(
            reviewMessage,
            topic,
            partition,
            offset,
            timestamp,
            record,
            acknowledgment
        );
        
        // Then
        verify(reviewProcessingService).processReviewMessage(reviewMessage);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void shouldNotAcknowledgeMessageWhenProcessingFails() {
        // Given
        ReviewMessage reviewMessage = createSampleReviewMessage();
        doThrow(new RuntimeException("Processing failed"))
            .when(reviewProcessingService).processReviewMessage(reviewMessage);
        String topic = "reviews";
        int partition = 0;
        long offset = 1L;
        long timestamp = System.currentTimeMillis();
        org.apache.kafka.clients.consumer.ConsumerRecord<String, com.reviewcore.dto.ReviewMessage> record = null;
        
        // When & Then
        try {
            reviewConsumer.consumeReviewMessage(
                reviewMessage,
                topic,
                partition,
                offset,
                timestamp,
                record,
                acknowledgment
            );
        } catch (RuntimeException e) {
            // Expected
        }
        
        verify(reviewProcessingService).processReviewMessage(reviewMessage);
        verify(acknowledgment, never()).acknowledge();
    }

    private ReviewMessage createSampleReviewMessage() {
        ReviewMessage message = new ReviewMessage();
        message.setHotelId(123l);
        message.setHotelName("Sample Hotel");
        message.setPlatform("Booking.com");
        
        ReviewMessage.ReviewComment comment = new ReviewMessage.ReviewComment();
        comment.setHotelReviewId(1L);
        comment.setProviderId(1234);
        comment.setRating(4.5);
        comment.setRatingText("Excellent");
        comment.setReviewTitle("Great stay");
        comment.setReviewComments("Amazing experience");
        comment.setReviewDate("2024-01-15T10:30:00Z");
        
        message.setComment(comment);
        
        return message;
    }
} 