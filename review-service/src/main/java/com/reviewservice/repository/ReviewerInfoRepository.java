package com.reviewservice.repository;

import com.reviewcore.model.ReviewerInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewerInfoRepository extends JpaRepository<ReviewerInfo, Long> {
} 