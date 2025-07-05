package com.reviewservice.repository;

import com.reviewcore.model.OverallProviderScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OverallProviderScoreRepository extends JpaRepository<OverallProviderScore, OverallProviderScore.OverallProviderScoreId> {
} 