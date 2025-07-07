package com.reviewconsumer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reviewcore.dto.ReviewMessage;
import com.reviewcore.model.BadReviewRecord;
import com.reviewcore.dto.BadReviewMessage;
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
    private final ReviewProcessingService reviewProcessingService;
    
    private final AtomicLong processedCount = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    
    @Autowired
    public ReviewConsumerService(ObjectMapper objectMapper, MetricsService metricsService, 
                               BadReviewRecordRepository badReviewRecordRepository,
                               ReviewProcessingService reviewProcessingService) {
        this.objectMapper = objectMapper;
        this.metricsService = metricsService;
        this.badReviewRecordRepository = badReviewRecordRepository;
        this.reviewProcessingService = reviewProcessingService;
    }
    
    @KafkaListener(
        topics = "${kafka.topic.reviews}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeReview(
            @Payload String reviewJson,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        try {
            logger.info("Received review from topic: {}, partition: {}, offset: {}", 
                       topic, partition, offset);
            logger.debug("Processing review JSON: {}", reviewJson);
            
            // Parse the JSON review
            ReviewMessage review = objectMapper.readValue(reviewJson, ReviewMessage.class);
            
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
        topics = "${kafka.topic.bad-reviews}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeBadReview(
            @Payload String payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        try {
            logger.info("Received bad review from topic: {}, partition: {}, offset: {}", 
                       topic, partition, offset);
            logger.debug("Processing bad review payload: {}", payload);

            BadReviewMessage badReviewMessage = objectMapper.readValue(payload, BadReviewMessage.class);
            processBadReview(badReviewMessage);
            metricsService.incrementBadReviews();
            acknowledgment.acknowledge();
            logger.info("Successfully processed bad review");
        } catch (Exception e) {
            long currentErrorCount = errorCount.incrementAndGet();
            metricsService.incrementErrorCount();
            logger.error("Error processing bad review from topic: {}, partition: {}, offset: {}. " +
                        "Error count: {}", topic, partition, offset, currentErrorCount, e);
            acknowledgment.acknowledge();
        }
    }
    
    private void processReview(ReviewMessage review) {
        logger.info("Processing review for hotelId: {} from platform: {}", 
                   review.getHotelId(), review.getPlatform());
        
        // Use the ReviewProcessingService to store the review in database
        reviewProcessingService.processReviewMessage(review);
        
        logger.info("Successfully stored review for hotelId: {} in database", review.getHotelId());
    }
    
    void processBadReview(BadReviewMessage badReviewMessage) {
        try {
            // Convert the BadReviewMessage to JSON string
            String jsonData = objectMapper.writeValueAsString(badReviewMessage);
            
            // Create BadReviewRecord entity with composite primary key
            BadReviewRecord badReviewRecord = new BadReviewRecord(
                badReviewMessage.getReviewId(), // review_id
                badReviewMessage.getProviderId(), // provider_id
                jsonData,
                badReviewMessage.getPlatform(),
                badReviewMessage.getReason()
            );
            
            // Save to database
            BadReviewRecord savedRecord = badReviewRecordRepository.save(badReviewRecord);
            
            logger.info("Stored bad review record in database with review ID: {} and provider ID: {} and reason: {}", 
                       savedRecord.getId().getReviewId(), savedRecord.getId().getProviderId(), badReviewMessage.getReason());
            
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