package com.reviewservice.service;

import com.reviewcore.model.EntityReview;
import com.reviewservice.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReviewService {
    private final ReviewRepository reviewRepository;

    @Autowired
    public ReviewService(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    public void processReview(EntityReview review) {
        // Idempotency: check if review already exists
        if (!reviewRepository.existsByReviewId(review.getReviewId())) {
            reviewRepository.save(review);
        }
    }
} 