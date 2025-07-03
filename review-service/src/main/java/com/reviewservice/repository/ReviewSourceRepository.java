package com.reviewservice.repository;

import com.reviewcore.model.ReviewSource;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewSourceRepository extends JpaRepository<ReviewSource, Long> {
} 