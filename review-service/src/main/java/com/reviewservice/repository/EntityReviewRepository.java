package com.reviewservice.repository;

import com.reviewcore.model.EntityReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface EntityReviewRepository extends JpaRepository<EntityReview, Long> {
    
    /**
     * Find reviews by platform
     */
    List<EntityReview> findByPlatform(String platform);
    
    /**
     * Find reviews by entity ID (hotel ID)
     */
    List<EntityReview> findByEntityId(Integer entityId);
    
    /**
     * Find reviews by rating range
     */
    List<EntityReview> findByRatingBetween(Double minRating, Double maxRating);
    
    /**
     * Find distinct platforms
     */
    @Query("SELECT DISTINCT e.platform FROM EntityReview e WHERE e.platform IS NOT NULL")
    List<String> findDistinctPlatforms();
    
    /**
     * Find average rating
     */
    @Query("SELECT AVG(e.rating) FROM EntityReview e WHERE e.rating IS NOT NULL")
    Double findAverageRating();
    
    /**
     * Find rating distribution
     */
    @Query("SELECT e.rating, COUNT(e) FROM EntityReview e WHERE e.rating IS NOT NULL GROUP BY e.rating ORDER BY e.rating")
    List<Object[]> findRatingDistribution();
    
    /**
     * Find review count by platform
     */
    @Query("SELECT e.platform, COUNT(e) FROM EntityReview e WHERE e.platform IS NOT NULL GROUP BY e.platform")
    List<Object[]> findReviewCountByPlatform();
} 