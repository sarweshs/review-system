package com.reviewservice.service;

import com.reviewcore.model.EntityReview;
import com.reviewservice.repository.EntityReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntityReviewService {
    private final EntityReviewRepository entityReviewRepository;
    private final BadReviewRecordService badReviewRecordService;

    public Map<String, Object> getAllReviewsBody(
            int page, int size, String sortBy, String sortDir, String platform, Double minRating, Double maxRating, String search
    ) {
        log.info("🔍 Fetching reviews from database - page: {}, size: {}, sortBy: {}, sortDir: {}, platform: {}, minRating: {}, maxRating: {}, search: {}", 
                page, size, sortBy, sortDir, platform, minRating, maxRating, search);
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<EntityReview> reviewsPage;
        
        // Apply filters based on provided parameters
        if (platform != null && !platform.isEmpty()) {
            if (minRating != null && maxRating != null) {
                if (search != null && !search.isEmpty()) {
                    // Platform + Rating Range + Search
                    reviewsPage = entityReviewRepository.findByPlatformAndRatingRangeAndSearchTerm(platform, minRating, maxRating, search, pageable);
                } else {
                    // Platform + Rating Range
                    reviewsPage = entityReviewRepository.findByPlatformAndRatingBetween(platform, minRating, maxRating, pageable);
                }
            } else {
                if (search != null && !search.isEmpty()) {
                    // Platform + Search
                    reviewsPage = entityReviewRepository.findByPlatformAndSearchTerm(platform, search, pageable);
                } else {
                    // Platform only
                    reviewsPage = entityReviewRepository.findByPlatform(platform, pageable);
                }
            }
        } else if (minRating != null && maxRating != null) {
            if (search != null && !search.isEmpty()) {
                // Rating Range + Search
                reviewsPage = entityReviewRepository.findByRatingRangeAndSearchTerm(minRating, maxRating, search, pageable);
            } else {
                // Rating Range only
                reviewsPage = entityReviewRepository.findByRatingBetween(minRating, maxRating, pageable);
            }
        } else if (search != null && !search.isEmpty()) {
            // Search only
            reviewsPage = entityReviewRepository.findBySearchTerm(search, pageable);
        } else {
            // No filters, get all reviews
            reviewsPage = entityReviewRepository.findAll(pageable);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("reviews", reviewsPage.getContent());
        response.put("currentPage", reviewsPage.getNumber());
        response.put("totalItems", reviewsPage.getTotalElements());
        response.put("totalPages", reviewsPage.getTotalPages());
        response.put("hasNext", reviewsPage.hasNext());
        response.put("hasPrevious", reviewsPage.hasPrevious());
        
        log.info("✅ Database query completed, returning {} reviews from page {} of {}", 
                reviewsPage.getContent().size(), reviewsPage.getNumber(), reviewsPage.getTotalPages());
        return response;
    }

    @Cacheable(value = "reviews", key = "'platform_' + #platform")
    public List<EntityReview> getReviewsByPlatformBody(String platform) {
        log.info("Fetching reviews for platform: {}", platform);
        return entityReviewRepository.findByPlatform(platform);
    }

    @Cacheable(value = "reviews", key = "'entity_' + #entityId")
    public List<EntityReview> getReviewsByEntityIdBody(Integer entityId) {
        log.info("Fetching reviews for entity ID: {}", entityId);
        return entityReviewRepository.findByEntityId(entityId);
    }

    @Cacheable(value = "reviews", key = "'rating_' + #minRating + '_' + #maxRating")
    public List<EntityReview> getReviewsByRatingRangeBody(Double minRating, Double maxRating) {
        log.info("Fetching reviews with rating between {} and {}", minRating, maxRating);
        return entityReviewRepository.findByRatingBetween(minRating, maxRating);
    }

    @Cacheable(value = "reviews", key = "'review_' + #reviewId")
    public EntityReview getReviewByIdBody(Long reviewId) {
        log.info("Fetching review with ID: {}", reviewId);
        return entityReviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found with ID: " + reviewId));
    }

    @Cacheable(value = "review-stats", key = "'good_statistics'")
    public Map<String, Object> getReviewStatisticsBody() {
        log.info("Fetching good review statistics");
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalReviews", entityReviewRepository.count());
        stats.put("platforms", entityReviewRepository.findDistinctPlatforms());
        stats.put("averageRating", entityReviewRepository.findAverageRating());
        stats.put("ratingDistribution", entityReviewRepository.findRatingDistribution());
        return stats;
    }

    @Cacheable(value = "review-summary", key = "'summary'")
    public Map<String, Object> getReviewSummaryBody() {
        log.info("Fetching combined review summary");
        Map<String, Object> summary = new HashMap<>();
        // Good reviews summary
        summary.put("totalGoodReviews", entityReviewRepository.count());
        summary.put("goodReviewsByPlatform", entityReviewRepository.findReviewCountByPlatform());
        // Bad reviews summary
        Map<String, Object> badStats = badReviewRecordService.getStatistics();
        summary.put("totalBadReviews", badStats.get("totalBadRecords"));
        summary.put("badReviewsByPlatform", badStats.get("byPlatform"));
        summary.put("badReviewsByReason", badStats.get("byReason"));
        return summary;
    }
} 