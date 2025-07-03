package com.reviewservice.service;

import com.reviewcore.model.Review;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.reviewservice.repository.ReviewRepository;

@Service
public class ReviewService {
    @Autowired
    private ReviewRepository reviewRepository;

    public void processReview(Review review) {
        // Idempotency: check if review already exists
        if (!reviewRepository.existsByReviewId(review.getReviewId())) {
            reviewRepository.save(review);
        }
    }
} 