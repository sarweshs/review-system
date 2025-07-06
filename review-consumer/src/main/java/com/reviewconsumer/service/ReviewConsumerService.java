package com.reviewconsumer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reviewcore.model.EntityReview;
import com.reviewcore.model.BadReviewRecord;
import com.reviewconsumer.repository.BadReviewRecordRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ReviewConsumerService {
    
    private static final Logger logger = LoggerFactory.getLogger(ReviewConsumerService.class);
    
    private final ObjectMapper objectMapper;
    private final MetricsService metricsService;
    private final BadReviewRecordRepository badReviewRecordRepository;
    
    private final AtomicLong processedCount = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    
    @Autowired
    public ReviewConsumerService(ObjectMapper objectMapper, MetricsService metricsService, 
                               BadReviewRecordRepository badReviewRecordRepository) {
        this.objectMapper = objectMapper;
        this.metricsService = metricsService;
        this.badReviewRecordRepository = badReviewRecordRepository;
    }
    
    @KafkaListener(
        topics = "${kafka.topics.reviews:reviews}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeReview(
            @Payload EntityReview review,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        try {
            logger.info("Received review from topic: {}, partition: {}, offset: {}", 
                       topic, partition, offset);
            logger.debug("Processing review: {}", review);
            
            // Process the review
            processReview(review);
            
            // Update metrics
            long currentCount = processedCount.incrementAndGet();
            metricsService.incrementProcessedReviews();
            
            // Acknowledge the message
            acknowledgment.acknowledge();
            
            logger.info("Successfully processed review. Total processed: {}", currentCount);
            
        } catch (Exception e) {
            long currentErrorCount = errorCount.incrementAndGet();
            metricsService.incrementErrorCount();
            
            logger.error("Error processing review from topic: {}, partition: {}, offset: {}. " +
                        "Error count: {}", topic, partition, offset, currentErrorCount, e);
            
            // In a real application, you might want to send to a dead letter queue
            // For now, we'll just log the error and acknowledge to avoid infinite retries
            acknowledgment.acknowledge();
        }
    }
    
    @KafkaListener(
        topics = "${kafka.topics.bad-reviews:bad_review_records}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeBadReview(
            @Payload com.reviewcore.dto.ReviewMessage badReviewMessage,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        try {
            logger.info("Received bad review from topic: {}, partition: {}, offset: {}", 
                       topic, partition, offset);
            logger.debug("Processing bad review: {}", badReviewMessage);
            
            // Process the bad review
            processBadReview(badReviewMessage);
            
            // Update metrics
            metricsService.incrementBadReviews();
            
            // Acknowledge the message
            acknowledgment.acknowledge();
            
            logger.info("Successfully processed bad review");
            
        } catch (Exception e) {
            long currentErrorCount = errorCount.incrementAndGet();
            metricsService.incrementErrorCount();
            
            logger.error("Error processing bad review from topic: {}, partition: {}, offset: {}. " +
                        "Error count: {}", topic, partition, offset, currentErrorCount, e);
            
            // Acknowledge to avoid infinite retries
            acknowledgment.acknowledge();
        }
    }
    
    private void processReview(EntityReview review) {
        // TODO: Implement actual review processing logic
        // This could include:
        // - Storing in database
        // - Sending to analytics
        // - Triggering notifications
        // - Data enrichment
        
        logger.info("Processing review for entity ID: {} from platform: {}", 
                   review.getEntityId(), review.getPlatform());
        
        // Simulate some processing time
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void processBadReview(com.reviewcore.dto.ReviewMessage badReviewMessage) {
        try {
            // Convert the ReviewMessage to JSON string
            String jsonData = objectMapper.writeValueAsString(badReviewMessage);
            
            // Create BadReviewRecord entity
            BadReviewRecord badReviewRecord = new BadReviewRecord(
                jsonData,
                badReviewMessage.getPlatform(),
                "Invalid review data structure or validation failure"
            );
            
            // Save to database
            BadReviewRecord savedRecord = badReviewRecordRepository.save(badReviewRecord);
            
            logger.info("Stored bad review record in database with ID: {}", savedRecord.getId());
            
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            logger.error("Failed to serialize bad review message to JSON: {}", badReviewMessage, e);
            throw new RuntimeException("Failed to serialize bad review message", e);
        } catch (Exception e) {
            logger.error("Failed to store bad review record in database: {}", badReviewMessage, e);
            throw e; // Re-throw to trigger error handling
        }
    }
    
    public long getProcessedCount() {
        return processedCount.get();
    }
    
    public long getErrorCount() {
        return errorCount.get();
    }
} 