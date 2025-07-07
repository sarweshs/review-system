package com.reviewservice.repository;

import com.reviewcore.model.EntityReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface EntityReviewRepository extends JpaRepository<EntityReview, EntityReview.EntityReviewId> {
    
    /**
     * Find reviews by platform
     */
    List<EntityReview> findByPlatform(String platform);
    
    /**
     * Find reviews by platform with pagination
     */
    Page<EntityReview> findByPlatform(String platform, Pageable pageable);
    
    /**
     * Find reviews by entity ID (hotel ID)
     */
    List<EntityReview> findByEntityId(Integer entityId);
    
    /**
     * Find reviews by rating range
     */
    List<EntityReview> findByRatingBetween(Double minRating, Double maxRating);
    
    /**
     * Find reviews by rating range with pagination
     */
    Page<EntityReview> findByRatingBetween(Double minRating, Double maxRating, Pageable pageable);
    
    /**
     * Find reviews by platform and rating range with pagination
     */
    Page<EntityReview> findByPlatformAndRatingBetween(String platform, Double minRating, Double maxRating, Pageable pageable);
    
    /**
     * Find reviews by search term in title or comments
     */
    @Query("SELECT e FROM EntityReview e WHERE LOWER(e.reviewTitle) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(e.reviewComments) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<EntityReview> findBySearchTerm(@Param("search") String search, Pageable pageable);
    
    /**
     * Find reviews by platform and search term
     */
    @Query("SELECT e FROM EntityReview e WHERE e.platform = :platform AND (LOWER(e.reviewTitle) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(e.reviewComments) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<EntityReview> findByPlatformAndSearchTerm(@Param("platform") String platform, @Param("search") String search, Pageable pageable);
    
    /**
     * Find reviews by rating range and search term
     */
    @Query("SELECT e FROM EntityReview e WHERE e.rating BETWEEN :minRating AND :maxRating AND (LOWER(e.reviewTitle) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(e.reviewComments) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<EntityReview> findByRatingRangeAndSearchTerm(@Param("minRating") Double minRating, @Param("maxRating") Double maxRating, @Param("search") String search, Pageable pageable);
    
    /**
     * Find reviews by platform, rating range, and search term
     */
    @Query("SELECT e FROM EntityReview e WHERE e.platform = :platform AND e.rating BETWEEN :minRating AND :maxRating AND (LOWER(e.reviewTitle) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(e.reviewComments) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<EntityReview> findByPlatformAndRatingRangeAndSearchTerm(@Param("platform") String platform, @Param("minRating") Double minRating, @Param("maxRating") Double maxRating, @Param("search") String search, Pageable pageable);
    
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