package com.reviewservice.controller;

import com.reviewcore.model.EntityReview;
import com.reviewservice.service.BadReviewRecordService;
import com.reviewservice.repository.EntityReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {
    
    private final EntityReviewRepository entityReviewRepository;
    private final BadReviewRecordService badReviewRecordService;
    
    /**
     * Get all good reviews with pagination
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllReviews(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sortBy", defaultValue = "reviewId") String sortBy,
            @RequestParam(name = "sortDir", defaultValue = "desc") String sortDir,
            @RequestParam(name = "platform", required = false) String platform,
            @RequestParam(name = "minRating", required = false) Double minRating,
            @RequestParam(name = "maxRating", required = false) Double maxRating,
            @RequestParam(name = "search", required = false) String search
    ) {
        return ResponseEntity.ok(getAllReviewsBody(page, size, sortBy, sortDir, platform, minRating, maxRating, search));
    }
    
    @Cacheable(value = "reviews", key = "'all_' + #page + '_' + #size + '_' + #sortBy + '_' + #sortDir")
    public Map<String, Object> getAllReviewsBody(
            int page, int size, String sortBy, String sortDir, String platform, Double minRating, Double maxRating, String search
    ) {
        log.info("Fetching reviews - page: {}, size: {}, sortBy: {}, sortDir: {}", page, size, sortBy, sortDir);
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<EntityReview> reviewsPage = entityReviewRepository.findAll(pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("reviews", reviewsPage.getContent());
        response.put("currentPage", reviewsPage.getNumber());
        response.put("totalItems", reviewsPage.getTotalElements());
        response.put("totalPages", reviewsPage.getTotalPages());
        response.put("hasNext", reviewsPage.hasNext());
        response.put("hasPrevious", reviewsPage.hasPrevious());
        
        return response;
    }
    
    /**
     * Get reviews by platform
     */
    @GetMapping("/platform/{platform}")
    public ResponseEntity<List<EntityReview>> getReviewsByPlatform(@PathVariable(name = "platform") String platform) {
        return ResponseEntity.ok(getReviewsByPlatformBody(platform));
    }
    
    @Cacheable(value = "reviews", key = "'platform_' + #platform")
    public List<EntityReview> getReviewsByPlatformBody(String platform) {
        log.info("Fetching reviews for platform: {}", platform);
        return entityReviewRepository.findByPlatform(platform);
    }
    
    /**
     * Get reviews by entity ID (hotel ID)
     */
    @GetMapping("/entity/{entityId}")
    public ResponseEntity<List<EntityReview>> getReviewsByEntityId(@PathVariable(name = "entityId") Integer entityId) {
        return ResponseEntity.ok(getReviewsByEntityIdBody(entityId));
    }
    
    @Cacheable(value = "reviews", key = "'entity_' + #entityId")
    public List<EntityReview> getReviewsByEntityIdBody(Integer entityId) {
        log.info("Fetching reviews for entity ID: {}", entityId);
        return entityReviewRepository.findByEntityId(entityId);
    }
    
    /**
     * Get reviews by rating range
     */
    @GetMapping("/rating")
    public ResponseEntity<List<EntityReview>> getReviewsByRatingRange(
            @RequestParam(name = "minRating") Double minRating,
            @RequestParam(name = "maxRating") Double maxRating) {
        return ResponseEntity.ok(getReviewsByRatingRangeBody(minRating, maxRating));
    }
    
    @Cacheable(value = "reviews", key = "'rating_' + #minRating + '_' + #maxRating")
    public List<EntityReview> getReviewsByRatingRangeBody(Double minRating, Double maxRating) {
        log.info("Fetching reviews with rating between {} and {}", minRating, maxRating);
        return entityReviewRepository.findByRatingBetween(minRating, maxRating);
    }
    
    /**
     * Get review by ID
     */
    @GetMapping("/{reviewId}")
    public ResponseEntity<EntityReview> getReviewById(@PathVariable(name = "reviewId") Long reviewId) {
        return ResponseEntity.ok(getReviewByIdBody(reviewId));
    }
    
    @Cacheable(value = "reviews", key = "'review_' + #reviewId")
    public EntityReview getReviewByIdBody(Long reviewId) {
        log.info("Fetching review with ID: {}", reviewId);
        return entityReviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found with ID: " + reviewId));
    }
    
    /**
     * Get review statistics (good reviews only)
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getReviewStatistics() {
        return ResponseEntity.ok(getReviewStatisticsBody());
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
    
    /**
     * Get combined review summary (good + bad)
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getReviewSummary() {
        return ResponseEntity.ok(getReviewSummaryBody());
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
        summary.put("totalBadReviews", badStats.get("totalRecords"));
        summary.put("badReviewsByPlatform", badStats.get("recordsByPlatform"));
        summary.put("badReviewsByReason", badStats.get("recordsByReason"));
        
        return summary;
    }
} 