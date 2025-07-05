package com.reviewservice.repository;

import com.reviewcore.model.EntityReview;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<EntityReview, Long> {
    boolean existsByReviewId(String reviewId);
} 