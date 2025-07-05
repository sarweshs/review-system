package com.reviewcore.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * DTO representing a review message from Kafka
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewMessage {
    
    @JsonProperty("hotelId")
    private Long hotelId;
    
    @JsonProperty("platform")
    private String platform;
    
    @JsonProperty("hotelName")
    private String hotelName;
    
    @JsonProperty("comment")
    private ReviewComment comment;
    
    @JsonProperty("overallByProviders")
    private List<OverallProvider> overallByProviders;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewComment {
        @JsonProperty("isShowReviewResponse")
        private Boolean isShowReviewResponse;
        
        @JsonProperty("hotelReviewId")
        private Long hotelReviewId;
        
        @JsonProperty("providerId")
        private Integer providerId;
        
        @JsonProperty("rating")
        private Double rating;
        
        @JsonProperty("checkInDateMonthAndYear")
        private String checkInDateMonthAndYear;
        
        @JsonProperty("encryptedReviewData")
        private String encryptedReviewData;
        
        @JsonProperty("formattedRating")
        private String formattedRating;
        
        @JsonProperty("formattedReviewDate")
        private String formattedReviewDate;
        
        @JsonProperty("ratingText")
        private String ratingText;
        
        @JsonProperty("responderName")
        private String responderName;
        
        @JsonProperty("responseDateText")
        private String responseDateText;
        
        @JsonProperty("responseTranslateSource")
        private String responseTranslateSource;
        
        @JsonProperty("reviewComments")
        private String reviewComments;
        
        @JsonProperty("reviewNegatives")
        private String reviewNegatives;
        
        @JsonProperty("reviewPositives")
        private String reviewPositives;
        
        @JsonProperty("reviewProviderLogo")
        private String reviewProviderLogo;
        
        @JsonProperty("reviewProviderText")
        private String reviewProviderText;
        
        @JsonProperty("reviewTitle")
        private String reviewTitle;
        
        @JsonProperty("translateSource")
        private String translateSource;
        
        @JsonProperty("translateTarget")
        private String translateTarget;
        
        @JsonProperty("reviewDate")
        private String reviewDate;
        
        @JsonProperty("reviewerInfo")
        private ReviewerInfoDto reviewerInfo;
        
        @JsonProperty("originalTitle")
        private String originalTitle;
        
        @JsonProperty("originalComment")
        private String originalComment;
        
        @JsonProperty("formattedResponseDate")
        private String formattedResponseDate;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewerInfoDto {
        @JsonProperty("countryName")
        private String countryName;
        
        @JsonProperty("displayMemberName")
        private String displayMemberName;
        
        @JsonProperty("flagName")
        private String flagName;
        
        @JsonProperty("reviewGroupName")
        private String reviewGroupName;
        
        @JsonProperty("roomTypeName")
        private String roomTypeName;
        
        @JsonProperty("countryId")
        private Integer countryId;
        
        @JsonProperty("lengthOfStay")
        private Integer lengthOfStay;
        
        @JsonProperty("reviewGroupId")
        private Integer reviewGroupId;
        
        @JsonProperty("roomTypeId")
        private Integer roomTypeId;
        
        @JsonProperty("reviewerReviewedCount")
        private Integer reviewerReviewedCount;
        
        @JsonProperty("isExpertReviewer")
        private Boolean isExpertReviewer;
        
        @JsonProperty("isShowGlobalIcon")
        private Boolean isShowGlobalIcon;
        
        @JsonProperty("isShowReviewedCount")
        private Boolean isShowReviewedCount;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OverallProvider {
        @JsonProperty("providerId")
        private Integer providerId;
        
        @JsonProperty("provider")
        private String provider;
        
        @JsonProperty("overallScore")
        private Double overallScore;
        
        @JsonProperty("reviewCount")
        private Integer reviewCount;
        
        @JsonProperty("grades")
        private Grades grades;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Grades {
        @JsonProperty("Cleanliness")
        private Double cleanliness;
        
        @JsonProperty("Facilities")
        private Double facilities;
        
        @JsonProperty("Location")
        private Double location;
        
        @JsonProperty("Room comfort and quality")
        private Double roomComfortQuality;
        
        @JsonProperty("Service")
        private Double service;
        
        @JsonProperty("Value for money")
        private Double valueForMoney;
    }
} 