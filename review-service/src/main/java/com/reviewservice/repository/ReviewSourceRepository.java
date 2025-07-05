package com.reviewservice.repository;

import com.reviewcore.model.ReviewSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewSourceRepository extends JpaRepository<ReviewSource, Long> {
    
    @Query("SELECT rs FROM ReviewSource rs WHERE rs.active = true")
    List<ReviewSource> findAllActive();
} 