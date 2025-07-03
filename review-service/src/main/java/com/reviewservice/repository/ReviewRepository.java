package com.reviewservice.repository;

import com.reviewcore.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    boolean existsByReviewId(String reviewId);
} 