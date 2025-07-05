package com.reviewservice.controller;

import com.reviewcore.model.BadReviewRecord;
import com.reviewservice.service.BadReviewRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/bad-review-records")
@RequiredArgsConstructor
public class BadReviewRecordController {
    
    private final BadReviewRecordService badReviewRecordService;
    
    /**
     * Get all bad review records
     */
    @GetMapping
    public ResponseEntity<List<BadReviewRecord>> getAllBadRecords() {
        log.info("Fetching all bad review records");
        List<BadReviewRecord> records = badReviewRecordService.getAllBadRecords();
        return ResponseEntity.ok(records);
    }
    
    /**
     * Get bad review record by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<BadReviewRecord> getBadRecordById(@PathVariable Long id) {
        log.info("Fetching bad review record with id: {}", id);
        BadReviewRecord record = badReviewRecordService.getBadRecordById(id);
        return ResponseEntity.ok(record);
    }
    
    /**
     * Get bad records by platform
     */
    @GetMapping("/platform/{platform}")
    public ResponseEntity<List<BadReviewRecord>> getBadRecordsByPlatform(@PathVariable String platform) {
        log.info("Fetching bad review records for platform: {}", platform);
        List<BadReviewRecord> records = badReviewRecordService.getBadRecordsByPlatform(platform);
        return ResponseEntity.ok(records);
    }
    
    /**
     * Get bad records by reason
     */
    @GetMapping("/reason")
    public ResponseEntity<List<BadReviewRecord>> getBadRecordsByReason(@RequestParam String reason) {
        log.info("Fetching bad review records with reason containing: {}", reason);
        List<BadReviewRecord> records = badReviewRecordService.getBadRecordsByReason(reason);
        return ResponseEntity.ok(records);
    }
    
    /**
     * Get bad records created after a specific date
     */
    @GetMapping("/after-date")
    public ResponseEntity<List<BadReviewRecord>> getBadRecordsAfterDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime date) {
        log.info("Fetching bad review records created after: {}", date);
        List<BadReviewRecord> records = badReviewRecordService.getBadRecordsAfterDate(date);
        return ResponseEntity.ok(records);
    }
    
    /**
     * Get recent bad records
     */
    @GetMapping("/recent")
    public ResponseEntity<List<BadReviewRecord>> getRecentBadRecords() {
        log.info("Fetching recent bad review records");
        List<BadReviewRecord> records = badReviewRecordService.getRecentBadRecords();
        return ResponseEntity.ok(records);
    }
    
    /**
     * Get statistics about bad review records
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        log.info("Fetching bad review records statistics");
        Map<String, Object> stats = badReviewRecordService.getStatistics();
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Get statistics for a specific platform
     */
    @GetMapping("/statistics/platform/{platform}")
    public ResponseEntity<Map<String, Object>> getStatisticsByPlatform(@PathVariable String platform) {
        log.info("Fetching bad review records statistics for platform: {}", platform);
        Map<String, Object> stats = badReviewRecordService.getStatisticsByPlatform(platform);
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Delete bad review record by ID
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBadRecord(@PathVariable Long id) {
        log.info("Deleting bad review record with id: {}", id);
        badReviewRecordService.deleteBadRecord(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Delete old bad records
     */
    @DeleteMapping("/cleanup")
    public ResponseEntity<Void> deleteOldRecords(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime cutoffDate) {
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