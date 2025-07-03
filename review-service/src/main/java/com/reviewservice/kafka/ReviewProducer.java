package com.reviewservice.kafka;

import com.reviewcore.model.Review;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class ReviewProducer {
    private static final String TOPIC = "reviews";

    @Autowired
    private KafkaTemplate<String, Review> kafkaTemplate;

    public void sendReview(Review review) {
        kafkaTemplate.send(TOPIC, review.getReviewId(), review);
    }
} 