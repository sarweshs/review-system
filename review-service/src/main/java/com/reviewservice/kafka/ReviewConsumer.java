package com.reviewservice.kafka;

import com.reviewcore.model.Review;
import com.reviewservice.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class ReviewConsumer {
    @Autowired
    private ReviewService reviewService;

    @KafkaListener(topics = "reviews", groupId = "review-processor")
    public void consume(Review review) {
        reviewService.processReview(review);
    }
} 