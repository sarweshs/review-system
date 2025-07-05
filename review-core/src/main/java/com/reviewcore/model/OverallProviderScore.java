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
    
    @Column(name = "overall_score")
    private Double overallScore;
    
    @Column(name = "review_count")
    private Integer reviewCount;
    
    @Column(name = "cleanliness")
    private Double cleanliness;
    
    @Column(name = "facilities")
    private Double facilities;
    
    @Column(name = "location")
    private Double location;
    
    @Column(name = "room_comfort_quality")
    private Double roomComfortQuality;
    
    @Column(name = "service")
    private Double service;
    
    @Column(name = "value_for_money")
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