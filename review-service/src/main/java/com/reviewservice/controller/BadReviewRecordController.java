package com.reviewservice.controller;

import com.reviewcore.model.BadReviewRecord;
import com.reviewservice.service.BadReviewRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/bad-review-records")
@CrossOrigin(origins = "*", allowedHeaders = "*", allowCredentials = "false")
@RequiredArgsConstructor
public class BadReviewRecordController {
    
    private final BadReviewRecordService badReviewRecordService;
    
    /**
     * Get all bad review records
     */
    @GetMapping
    public ResponseEntity<List<BadReviewRecord>> getAllBadRecords() {
        return ResponseEntity.ok(badReviewRecordService.getAllBadRecords());
    }
    
    /**
     * Get bad review record by review ID and provider ID
     */
    @GetMapping("/{reviewId}/{providerId}")
    public ResponseEntity<BadReviewRecord> getBadRecordById(@PathVariable(name = "reviewId") Long reviewId, 
                                                           @PathVariable(name = "providerId") Integer providerId) {
        try {
            return ResponseEntity.ok(badReviewRecordService.getBadRecordById(reviewId, providerId));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                log.warn("Bad review record not found with review ID: {} and provider ID: {}", reviewId, providerId);
                return ResponseEntity.notFound().build();
            }
            throw e; // Re-throw other runtime exceptions
        }
    }
    
    /**
     * Get bad records by platform
     */
    @GetMapping("/platform/{platform}")
    public ResponseEntity<List<BadReviewRecord>> getBadRecordsByPlatform(@PathVariable(name = "platform") String platform) {
        return ResponseEntity.ok(badReviewRecordService.getBadRecordsByPlatform(platform));
    }
    
    /**
     * Get bad records by reason
     */
    @GetMapping("/reason")
    public ResponseEntity<List<BadReviewRecord>> getBadRecordsByReason(@RequestParam(name = "reason") String reason) {
        return ResponseEntity.ok(badReviewRecordService.getBadRecordsByReason(reason));
    }
    
    /**
     * Get bad records created after a specific date
     */
    @GetMapping("/after-date")
    public ResponseEntity<List<BadReviewRecord>> getBadRecordsAfterDate(
            @RequestParam(name = "date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date) {
        return ResponseEntity.ok(badReviewRecordService.getBadRecordsAfterDate(date));
    }
    
    /**
     * Get recent bad records
     */
    @GetMapping("/recent")
    public ResponseEntity<List<BadReviewRecord>> getRecentBadRecords() {
        return ResponseEntity.ok(badReviewRecordService.getRecentBadRecords());
    }
    
    /**
     * Get statistics about bad review records
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        return ResponseEntity.ok(badReviewRecordService.getStatistics());
    }
    
    /**
     * Get statistics for a specific platform
     */
    @GetMapping("/statistics/platform/{platform}")
    public ResponseEntity<Map<String, Object>> getStatisticsByPlatform(@PathVariable(name = "platform") String platform) {
        return ResponseEntity.ok(badReviewRecordService.getStatisticsByPlatform(platform));
    }
    
    /**
     * Delete bad review record by review ID and provider ID
     */
    @DeleteMapping("/{reviewId}/{providerId}")
    public ResponseEntity<Void> deleteBadRecord(@PathVariable(name = "reviewId") Long reviewId, 
                                               @PathVariable(name = "providerId") Integer providerId) {
        try {
            log.info("Deleting bad review record with review ID: {} and provider ID: {}", reviewId, providerId);
            badReviewRecordService.deleteBadRecord(reviewId, providerId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                log.warn("Bad review record not found for deletion with review ID: {} and provider ID: {}", reviewId, providerId);
                return ResponseEntity.notFound().build();
            }
            throw e; // Re-throw other runtime exceptions
        }
    }
    
    /**
     * Delete old bad records
     */
    @DeleteMapping("/cleanup")
    public ResponseEntity<Void> deleteOldRecords(
            @RequestParam(name = "cutoffDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime cutoffDate) {
        log.info("Deleting bad review records older than: {}", cutoffDate);
        badReviewRecordService.deleteOldRecords(cutoffDate);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Create a new bad review record
     */
    @PostMapping
    public ResponseEntity<BadReviewRecord> createBadRecord(@RequestBody BadReviewRecordRequest request) {
        log.info("Creating bad review record for platform: {}, reason: {}", request.getPlatform(), request.getReason());
        BadReviewRecord record = badReviewRecordService.saveBadRecord(
                request.getJsonData(), 
                request.getPlatform(), 
                request.getReason()
        );
        return ResponseEntity.ok(record);
    }
    
    /**
     * Request DTO for creating bad review records
     */
    public static class BadReviewRecordRequest {
        private String jsonData;
        private String platform;
        private String reason;
        
        // Getters and Setters
        public String getJsonData() {
            return jsonData;
        }
        
        public void setJsonData(String jsonData) {
            this.jsonData = jsonData;
        }
        
        public String getPlatform() {
            return platform;
        }
        
        public void setPlatform(String platform) {
            this.platform = platform;
        }
        
        public String getReason() {
            return reason;
        }
        
        public void setReason(String reason) {
            this.reason = reason;
        }
    }
} 