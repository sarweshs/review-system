package com.reviewcore.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Model representing reviewer information
 */
@Entity
@Table(name = "reviewer_info")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewerInfo {
    
    @EmbeddedId
    private ReviewerInfoId id;
    
    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewerInfoId implements java.io.Serializable {
        
        @Column(name = "review_id")
        private Long reviewId;
        
        @Column(name = "provider_id")
        private Integer providerId;
    }
    
    @Column(name = "country_id")
    private Integer countryId;
    
    @Column(name = "country_name")
    private String countryName;
    
    @Column(name = "flag_name")
    private String flagName;
    
    @Column(name = "review_group_id")
    private Integer reviewGroupId;
    
    @Column(name = "review_group_name")
    private String reviewGroupName;
    
    @Column(name = "room_type_id")
    private Integer roomTypeId;
    
    @Column(name = "room_type_name")
    private String roomTypeName;
    
    @Column(name = "length_of_stay")
    private Integer lengthOfStay;
    
    @Column(name = "reviewer_reviewed_count")
    private Integer reviewerReviewedCount;
    
    @Column(name = "is_expert_reviewer")
    private Boolean isExpertReviewer;
    
    @Column(name = "is_show_global_icon")
    private Boolean isShowGlobalIcon;
    
    @Column(name = "is_show_reviewed_count")
    private Boolean isShowReviewedCount;
} 