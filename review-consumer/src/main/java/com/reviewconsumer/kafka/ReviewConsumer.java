package com.reviewconsumer.kafka;

import com.reviewconsumer.service.ReviewProcessingService;
import com.reviewcore.dto.ReviewMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewConsumer {

    private final ReviewProcessingService reviewProcessingService;

    @KafkaListener(
        topics = "${spring.kafka.consumer.topics.reviews}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeReviewMessage(
            @Payload ReviewMessage reviewMessage,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(KafkaHeaders.RECEIVED_TIMESTAMP) long timestamp,
            ConsumerRecord<String, ReviewMessage> record,
            Acknowledgment acknowledgment) {
        
        try {
            log.debug("Received review message from topic: {}, partition: {}, offset: {}, hotelId: {}, platform: {}", 
                     topic, partition, offset, reviewMessage.getHotelId(), reviewMessage.getPlatform());
            
            // Process the review message
            reviewProcessingService.processReviewMessage(reviewMessage);
            
            // Acknowledge the message
            acknowledgment.acknowledge();
            
            log.debug("Successfully processed and acknowledged review message from topic: {}, partition: {}, offset: {}", 
                     topic, partition, offset);
            
        } catch (Exception e) {
            log.error("Error processing review message from topic: {}, partition: {}, offset: {}, hotelId: {}", 
                     topic, partition, offset, reviewMessage.getHotelId(), e);
            
            // Don't acknowledge the message - it will be retried
            // In a production environment, you might want to implement dead letter queue logic here
            throw e;
        }
    }
} 