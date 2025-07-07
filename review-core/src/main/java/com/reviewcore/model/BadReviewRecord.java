package com.reviewcore.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

/**
 * Entity representing invalid review records that failed validation
 */
@Entity
@Table(name = "bad_review_records")
public class BadReviewRecord {
    
    @EmbeddedId
    private BadReviewRecordId id;
    
    @Embeddable
    public static class BadReviewRecordId implements java.io.Serializable {
        
        @Column(name = "review_id")
        private Long reviewId;
        
        @Column(name = "provider_id")
        private Integer providerId;
        
        // Default constructor
        public BadReviewRecordId() {}
        
        // Constructor with parameters
        public BadReviewRecordId(Long reviewId, Integer providerId) {
            this.reviewId = reviewId;
            this.providerId = providerId;
        }
        
        // Getters and Setters
        public Long getReviewId() {
            return reviewId;
        }
        
        public void setReviewId(Long reviewId) {
            this.reviewId = reviewId;
        }
        
        public Integer getProviderId() {
            return providerId;
        }
        
        public void setProviderId(Integer providerId) {
            this.providerId = providerId;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            
            BadReviewRecordId that = (BadReviewRecordId) o;
            
            if (reviewId != null ? !reviewId.equals(that.reviewId) : that.reviewId != null) return false;
            return providerId != null ? providerId.equals(that.providerId) : that.providerId != null;
        }
        
        @Override
        public int hashCode() {
            int result = reviewId != null ? reviewId.hashCode() : 0;
            result = 31 * result + (providerId != null ? providerId.hashCode() : 0);
            return result;
        }
    }
    
    @Column(name = "json_data", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private String jsonData;
    
    @Column(name = "platform", length = 100, nullable = false)
    private String platform;
    
    @Column(name = "reason", length = 500, nullable = false)
    private String reason;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    // Default constructor
    public BadReviewRecord() {
        this.createdAt = LocalDateTime.now(java.time.ZoneOffset.UTC);
    }
    
    // Constructor with parameters
    public BadReviewRecord(String jsonData, String platform, String reason) {
        this();
        this.jsonData = jsonData;
        this.platform = platform;
        this.reason = reason;
    }
    
    // Constructor with composite key
    public BadReviewRecord(Long reviewId, Integer providerId, String jsonData, String platform, String reason) {
        this();
        this.id = new BadReviewRecordId(reviewId, providerId);
        this.jsonData = jsonData;
        this.platform = platform;
        this.reason = reason;
    }
    
    // Getters and Setters
    public BadReviewRecordId getId() {
        return id;
    }
    
    public void setId(BadReviewRecordId id) {
        this.id = id;
    }
    
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
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    @Override
    public String toString() {
        return "BadReviewRecord{" +
                "id=" + id +
                ", jsonData='" + jsonData + '\'' +
                ", platform='" + platform + '\'' +
                ", reason='" + reason + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        BadReviewRecord that = (BadReviewRecord) o;
        
        return id != null ? id.equals(that.id) : that.id == null;
    }
    
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
} 