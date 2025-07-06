package com.reviewconsumer.repository;

import com.reviewcore.model.BadReviewRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BadReviewRecordRepository extends JpaRepository<BadReviewRecord, Long> {
} 