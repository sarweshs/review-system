package com.reviewservice.repository;

import com.reviewcore.model.BadReviewRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BadReviewRecordRepository extends JpaRepository<BadReviewRecord, Long> {
    
    /**
     * Find bad records by platform
     */
    List<BadReviewRecord> findByPlatform(String platform);
    
    /**
     * Find bad records by reason
     */
    List<BadReviewRecord> findByReasonContainingIgnoreCase(String reason);
    
    /**
     * Find bad records created after a specific date
     */
    List<BadReviewRecord> findByCreatedAtAfter(LocalDateTime date);
    
    /**
     * Find bad records by platform and created after a specific date
     */
    List<BadReviewRecord> findByPlatformAndCreatedAtAfter(String platform, LocalDateTime date);
    
    /**
     * Count total bad records
     */
    @Query("SELECT COUNT(b) FROM BadReviewRecord b")
    long countTotalBadRecords();
    
    /**
     * Count bad records by platform
     */
    @Query("SELECT b.platform, COUNT(b) FROM BadReviewRecord b GROUP BY b.platform")
    List<Object[]> countByPlatform();
    
    /**
     * Count bad records by reason
     */
    @Query("SELECT b.reason, COUNT(b) FROM BadReviewRecord b GROUP BY b.reason ORDER BY COUNT(b) DESC")
    List<Object[]> countByReason();
    
    /**
     * Count bad records created in the last N days
     */
    @Query("SELECT COUNT(b) FROM BadReviewRecord b WHERE b.createdAt >= :startDate")
    long countByCreatedAtAfter(@Param("startDate") LocalDateTime startDate);
    
    /**
     * Get recent bad records (last N records)
     */
    @Query("SELECT b FROM BadReviewRecord b ORDER BY b.createdAt DESC")
    List<BadReviewRecord> findRecentBadRecords();
    
    /**
     * Delete bad records older than a specific date
     */
    @Query("DELETE FROM BadReviewRecord b WHERE b.createdAt < :cutoffDate")
    void deleteOldRecords(@Param("cutoffDate") LocalDateTime cutoffDate);
} 