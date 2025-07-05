package com.reviewconsumer.repository;

import com.reviewcore.model.EntityReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EntityReviewRepository extends JpaRepository<EntityReview, Long> {
} 