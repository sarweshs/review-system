package com.reviewservice.controller;

import com.reviewcore.model.EntityReview;
import com.reviewservice.service.EntityReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(origins = "*", allowedHeaders = "*", allowCredentials = "false")
@RequiredArgsConstructor
public class ReviewController {
    
    private final EntityReviewService entityReviewService;
    
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
        return ResponseEntity.ok(entityReviewService.getAllReviewsBody(page, size, sortBy, sortDir, platform, minRating, maxRating, search));
    }
    
    /**
     * Get reviews by platform
     */
    @GetMapping("/platform/{platform}")
    public ResponseEntity<List<EntityReview>> getReviewsByPlatform(@PathVariable(name = "platform") String platform) {
        return ResponseEntity.ok(entityReviewService.getReviewsByPlatformBody(platform));
    }
    
    /**
     * Get reviews by entity ID (hotel ID)
     */
    @GetMapping("/entity/{entityId}")
    public ResponseEntity<List<EntityReview>> getReviewsByEntityId(@PathVariable(name = "entityId") Integer entityId) {
        return ResponseEntity.ok(entityReviewService.getReviewsByEntityIdBody(entityId));
    }
    
    /**
     * Get reviews by rating range
     */
    @GetMapping("/rating")
    public ResponseEntity<List<EntityReview>> getReviewsByRatingRange(
            @RequestParam(name = "minRating") Double minRating,
            @RequestParam(name = "maxRating") Double maxRating) {
        return ResponseEntity.ok(entityReviewService.getReviewsByRatingRangeBody(minRating, maxRating));
    }
    
    /**
     * Get review by ID and provider ID
     */
    @GetMapping("/{reviewId}/{providerId}")
    public ResponseEntity<EntityReview> getReviewById(@PathVariable(name = "reviewId") Long reviewId, 
                                                     @PathVariable(name = "providerId") Integer providerId) {
        return ResponseEntity.ok(entityReviewService.getReviewByIdBody(reviewId, providerId));
    }
    
    /**
     * Get review statistics (good reviews only)
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getReviewStatistics() {
        return ResponseEntity.ok(entityReviewService.getReviewStatisticsBody());
    }
    
    /**
     * Get combined review summary (good + bad)
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getReviewSummary() {
        return ResponseEntity.ok(entityReviewService.getReviewSummaryBody());
    }
} 