package com.reviewservice.service;

import com.reviewcore.dto.ReviewMessage;
import com.reviewcore.model.ReviewEntity;
import com.reviewcore.model.EntityType;
import com.reviewcore.model.EntityReview;
import com.reviewcore.model.ReviewerInfo;
import com.reviewcore.model.OverallProviderScore;
import com.reviewservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewProcessingService {
    
    private final EntityRepository entityRepository;
    private final EntityReviewRepository entityReviewRepository;
    private final ReviewerInfoRepository reviewerInfoRepository;
    private final OverallProviderScoreRepository overallProviderScoreRepository;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
    
    /**
     * Process a review message from Kafka
     */
    @Transactional
    public void processReviewMessage(ReviewMessage reviewMessage) {
        try {
            log.debug("Processing review message for hotelId: {}, platform: {}", 
                     reviewMessage.getHotelId(), reviewMessage.getPlatform());
            
            // Extract entity type from hotelId
            EntityType entityType = EntityType.fromId("hotelId");
            
            // Find or create entity using hotelId as entity_id (cast Long to Integer)
            ReviewEntity entity = findOrCreateEntity(reviewMessage.getHotelId().intValue(), reviewMessage.getHotelName(), entityType);
            
            // Process the review
            if (reviewMessage.getComment() != null) {
                processReview(reviewMessage, entity);
            }
            
            // Process overall provider scores
            if (reviewMessage.getOverallByProviders() != null && reviewMessage.getComment() != null) {
                processOverallProviderScores(reviewMessage.getOverallByProviders(), entity.getEntityId(), reviewMessage.getComment().getHotelReviewId(), reviewMessage.getComment().getProviderId());
            }
            
            log.debug("Successfully processed review message for entity: {}", entity.getEntityName());
            
        } catch (Exception e) {
            log.error("Error processing review message for hotelId: {}", reviewMessage.getHotelId(), e);
            throw e;
        }
    }
    
    /**
     * Find or create an entity
     */
    private ReviewEntity findOrCreateEntity(Integer hotelId, String entityName, EntityType entityType) {
        // First try to find by entity_id (hotelId)
        Optional<ReviewEntity> existingEntity = entityRepository.findById(hotelId);
        
        if (existingEntity.isPresent()) {
            log.debug("Found existing entity with ID: {} ({})", hotelId, entityName);
            return existingEntity.get();
        }
        
        // Create new entity with hotelId as entity_id
        ReviewEntity newEntity = new ReviewEntity();
        newEntity.setEntityId(hotelId);
        newEntity.setEntityName(entityName);
        newEntity.setEntityType(entityType);
        
        ReviewEntity savedEntity = entityRepository.save(newEntity);
        log.info("Created new entity: {} ({}) with ID: {}", entityName, entityType, savedEntity.getEntityId());
        
        return savedEntity;
    }
    
    /**
     * Process the review data
     */
    private void processReview(ReviewMessage reviewMessage, ReviewEntity entity) {
        ReviewMessage.ReviewComment comment = reviewMessage.getComment();
        
        // Create entity review with composite primary key
        EntityReview entityReview = new EntityReview();
        EntityReview.EntityReviewId reviewId = new EntityReview.EntityReviewId();
        reviewId.setReviewId(comment.getHotelReviewId());
        reviewId.setProviderId(comment.getProviderId());
        entityReview.setId(reviewId);
        
        entityReview.setEntityId(entity.getEntityId());
        entityReview.setPlatform(reviewMessage.getPlatform());
        entityReview.setRating(comment.getRating());
        entityReview.setRatingText(comment.getRatingText());
        entityReview.setReviewTitle(comment.getReviewTitle());
        entityReview.setReviewComments(comment.getReviewComments());
        entityReview.setReviewPositives(comment.getReviewPositives());
        entityReview.setReviewNegatives(comment.getReviewNegatives());
        entityReview.setCheckInDate(comment.getCheckInDateMonthAndYear());
        entityReview.setReviewDate(parseReviewDate(comment.getReviewDate()));
        entityReview.setResponderName(comment.getResponderName());
        entityReview.setResponseDate(comment.getResponseDateText());
        entityReview.setResponseText(comment.getResponseTranslateSource());
        entityReview.setReviewProviderText(comment.getReviewProviderText());
        entityReview.setReviewProviderLogo(comment.getReviewProviderLogo());
        entityReview.setEncryptedReviewData(comment.getEncryptedReviewData());
        entityReview.setOriginalTitle(comment.getOriginalTitle());
        entityReview.setOriginalComment(comment.getOriginalComment());
        
        entityReviewRepository.save(entityReview);
        log.debug("Saved entity review with ID: {}", entityReview.getId().getReviewId());
        
        // Process reviewer info
        if (comment.getReviewerInfo() != null) {
            processReviewerInfo(comment.getReviewerInfo(), comment.getHotelReviewId(), comment.getProviderId());
        }
    }
    
    /**
     * Process reviewer information
     */
    private void processReviewerInfo(ReviewMessage.ReviewerInfoDto reviewerInfoDto, Long reviewId, Integer providerId) {
        ReviewerInfo reviewerInfo = new ReviewerInfo();
        ReviewerInfo.ReviewerInfoId infoId = new ReviewerInfo.ReviewerInfoId();
        infoId.setReviewId(reviewId);
        infoId.setProviderId(providerId);
        reviewerInfo.setId(infoId);
        
        reviewerInfo.setCountryId(reviewerInfoDto.getCountryId());
        reviewerInfo.setCountryName(reviewerInfoDto.getCountryName());
        reviewerInfo.setFlagName(reviewerInfoDto.getFlagName());
        reviewerInfo.setReviewGroupId(reviewerInfoDto.getReviewGroupId());
        reviewerInfo.setReviewGroupName(reviewerInfoDto.getReviewGroupName());
        reviewerInfo.setRoomTypeId(reviewerInfoDto.getRoomTypeId());
        reviewerInfo.setRoomTypeName(reviewerInfoDto.getRoomTypeName());
        reviewerInfo.setLengthOfStay(reviewerInfoDto.getLengthOfStay());
        reviewerInfo.setReviewerReviewedCount(reviewerInfoDto.getReviewerReviewedCount());
        reviewerInfo.setIsExpertReviewer(reviewerInfoDto.getIsExpertReviewer());
        reviewerInfo.setIsShowGlobalIcon(reviewerInfoDto.getIsShowGlobalIcon());
        reviewerInfo.setIsShowReviewedCount(reviewerInfoDto.getIsShowReviewedCount());
        
        reviewerInfoRepository.save(reviewerInfo);
        log.debug("Saved reviewer info for review ID: {} and provider ID: {}", reviewId, providerId);
    }
    
    /**
     * Process overall provider scores
     */
    private void processOverallProviderScores(java.util.List<ReviewMessage.OverallProvider> providers, Integer entityId, Long reviewId, Integer reviewProviderId) {
        for (ReviewMessage.OverallProvider provider : providers) {
            OverallProviderScore.OverallProviderScoreId scoreId = new OverallProviderScore.OverallProviderScoreId();
            scoreId.setProviderId(reviewProviderId);
            scoreId.setReviewId(reviewId);
            
            OverallProviderScore score = new OverallProviderScore();
            score.setId(scoreId);
            score.setEntityId(entityId);
            score.setProvider(provider.getProvider());
            score.setOverallScore(provider.getOverallScore());
            score.setReviewCount(provider.getReviewCount());
            
            if (provider.getGrades() != null) {
                score.setCleanliness(provider.getGrades().getCleanliness());
                score.setFacilities(provider.getGrades().getFacilities());
                score.setLocation(provider.getGrades().getLocation());
                score.setRoomComfortQuality(provider.getGrades().getRoomComfortQuality());
                score.setService(provider.getGrades().getService());
                score.setValueForMoney(provider.getGrades().getValueForMoney());
            }
            
            overallProviderScoreRepository.save(score);
            log.debug("Saved overall provider score for entity: {}, provider: {}, review: {}, using review provider_id: {}", 
                     entityId, provider.getProvider(), reviewId, reviewProviderId);
        }
    }
    
    /**
     * Parse review date string to LocalDateTime
     */
    private LocalDateTime parseReviewDate(String reviewDateStr) {
        if (reviewDateStr == null || reviewDateStr.trim().isEmpty()) {
            return null;
        }
        
        try {
            return LocalDateTime.parse(reviewDateStr, DATE_FORMATTER);
        } catch (Exception e) {
            log.warn("Could not parse review date: {}", reviewDateStr, e);
            return null;
        }
    }
} 