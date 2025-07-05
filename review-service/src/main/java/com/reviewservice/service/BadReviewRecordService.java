package com.reviewservice.service;

import com.reviewcore.model.BadReviewRecord;
import com.reviewservice.repository.BadReviewRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BadReviewRecordService {
    
    private final BadReviewRecordRepository badReviewRecordRepository;
    
    /**
     * Save a bad review record
     */
    @Transactional
    public BadReviewRecord saveBadRecord(String jsonData, String platform, String reason) {
        BadReviewRecord badRecord = new BadReviewRecord(jsonData, platform, reason);
        BadReviewRecord saved = badReviewRecordRepository.save(badRecord);
        log.info("Saved bad review record for platform: {}, reason: {}", platform, reason);
        return saved;
    }
    
    /**
     * Get all bad review records
     */
    public List<BadReviewRecord> getAllBadRecords() {
        return badReviewRecordRepository.findAll();
    }
    
    /**
     * Get bad review record by ID
     */
    public BadReviewRecord getBadRecordById(Long id) {
        return badReviewRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bad review record not found with id: " + id));
    }
    
    /**
     * Get bad records by platform
     */
    public List<BadReviewRecord> getBadRecordsByPlatform(String platform) {
        return badReviewRecordRepository.findByPlatform(platform);
    }
    
    /**
     * Get bad records by reason
     */
    public List<BadReviewRecord> getBadRecordsByReason(String reason) {
        return badReviewRecordRepository.findByReasonContainingIgnoreCase(reason);
    }
    
    /**
     * Get bad records created after a specific date
     */
    public List<BadReviewRecord> getBadRecordsAfterDate(LocalDateTime date) {
        return badReviewRecordRepository.findByCreatedAtAfter(date);
    }
    
    /**
     * Get recent bad records
     */
    public List<BadReviewRecord> getRecentBadRecords() {
        return badReviewRecordRepository.findRecentBadRecords();
    }
    
    /**
     * Delete bad review record by ID
     */
    @Transactional
    public void deleteBadRecord(Long id) {
        if (badReviewRecordRepository.existsById(id)) {
            badReviewRecordRepository.deleteById(id);
            log.info("Deleted bad review record with id: {}", id);
        } else {
            throw new RuntimeException("Bad review record not found with id: " + id);
        }
    }
    
    /**
     * Delete old bad records
     */
    @Transactional
    public void deleteOldRecords(LocalDateTime cutoffDate) {
        badReviewRecordRepository.deleteOldRecords(cutoffDate);
        log.info("Deleted bad review records older than: {}", cutoffDate);
    }
    
    /**
     * Get statistics about bad review records
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // Total count
        stats.put("totalBadRecords", badReviewRecordRepository.countTotalBadRecords());
        
        // Count by platform
        List<Object[]> platformStats = badReviewRecordRepository.countByPlatform();
        Map<String, Long> platformCounts = new HashMap<>();
        for (Object[] result : platformStats) {
            platformCounts.put((String) result[0], (Long) result[1]);
        }
        stats.put("byPlatform", platformCounts);
        
        // Count by reason
        List<Object[]> reasonStats = badReviewRecordRepository.countByReason();
        Map<String, Long> reasonCounts = new HashMap<>();
        for (Object[] result : reasonStats) {
            reasonCounts.put((String) result[0], (Long) result[1]);
        }
        stats.put("byReason", reasonCounts);
        
        // Recent activity (last 7 days)
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        stats.put("last7Days", badReviewRecordRepository.countByCreatedAtAfter(weekAgo));
        
        return stats;
    }
    
    /**
     * Get statistics for a specific platform
     */
    public Map<String, Object> getStatisticsByPlatform(String platform) {
        Map<String, Object> stats = new HashMap<>();
        
        List<BadReviewRecord> platformRecords = badReviewRecordRepository.findByPlatform(platform);
        stats.put("totalRecords", (long) platformRecords.size());
        
        // Count by reason for this platform
        Map<String, Long> reasonCounts = new HashMap<>();
        for (BadReviewRecord record : platformRecords) {
            reasonCounts.merge(record.getReason(), 1L, Long::sum);
        }
        stats.put("byReason", reasonCounts);
        
        // Recent activity (last 7 days)
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        List<BadReviewRecord> recentRecords = badReviewRecordRepository.findByPlatformAndCreatedAtAfter(platform, weekAgo);
        stats.put("last7Days", (long) recentRecords.size());
        
        return stats;
    }
} 