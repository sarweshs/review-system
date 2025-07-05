package com.reviewcore.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Model representing entity reviews
 */
@Entity
@Table(name = "entity_reviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntityReview {
    
    @Id
    @Column(name = "review_id")
    private Long reviewId;
    
    @Column(name = "entity_id", nullable = false)
    private Integer entityId;
    
    @Column(name = "platform")
    private String platform;
    
    @Column(name = "provider_id")
    private Integer providerId;
    
    @Column(name = "rating", columnDefinition = "numeric(3,1)")
    private Double rating;
    
    @Column(name = "rating_text")
    private String ratingText;
    
    @Column(name = "review_title")
    private String reviewTitle;
    
    @Column(name = "review_comments")
    private String reviewComments;
    
    @Column(name = "review_positives")
    private String reviewPositives;
    
    @Column(name = "review_negatives")
    private String reviewNegatives;
    
    @Column(name = "check_in_date")
    private String checkInDate;
    
    @Column(name = "review_date")
    private java.time.LocalDateTime reviewDate;
    
    @Column(name = "responder_name")
    private String responderName;
    
    @Column(name = "response_date")
    private String responseDate;
    
    @Column(name = "response_text")
    private String responseText;
    
    @Column(name = "review_provider_text")
    private String reviewProviderText;
    
    @Column(name = "review_provider_logo")
    private String reviewProviderLogo;
    
    @Column(name = "encrypted_review_data")
    private String encryptedReviewData;
    
    @Column(name = "original_title")
    private String originalTitle;
    
    @Column(name = "original_comment")
    private String originalComment;
} 