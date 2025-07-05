package com.reviewcore.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Model representing overall provider scores
 */
@Entity
@Table(name = "overall_provider_scores")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OverallProviderScore {
    
    @EmbeddedId
    private OverallProviderScoreId id;
    
    @Column(name = "provider")
    private String provider;
    
    @Column(name = "overall_score", columnDefinition = "numeric(3,1)")
    private Double overallScore;
    
    @Column(name = "review_count")
    private Integer reviewCount;
    
    @Column(name = "cleanliness", columnDefinition = "numeric(3,1)")
    private Double cleanliness;
    
    @Column(name = "facilities", columnDefinition = "numeric(3,1)")
    private Double facilities;
    
    @Column(name = "location", columnDefinition = "numeric(3,1)")
    private Double location;
    
    @Column(name = "room_comfort_quality", columnDefinition = "numeric(3,1)")
    private Double roomComfortQuality;
    
    @Column(name = "service", columnDefinition = "numeric(3,1)")
    private Double service;
    
    @Column(name = "value_for_money",columnDefinition = "numeric(3,1)")
    private Double valueForMoney;
    
    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OverallProviderScoreId implements java.io.Serializable {
        
        @Column(name = "entity_id")
        private Integer entityId;
        
        @Column(name = "provider_id")
        private Integer providerId;
    }
} 