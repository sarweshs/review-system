package com.reviewautomation.util;

import com.reviewcore.dto.ReviewMessage;
import com.reviewcore.model.EntityReview;
import com.reviewcore.model.ReviewEntity;
import com.reviewcore.model.EntityType;
import com.reviewcore.model.ReviewerInfo;
import com.reviewcore.model.OverallProviderScore;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class for building test data
 */
public class TestDataBuilder {

    /**
     * Create a test ReviewMessage
     */
    public static ReviewMessage createTestReviewMessage() {
        ReviewMessage message = new ReviewMessage();
        message.setHotelId(12345L);
        message.setHotelName("Test Hotel");
        message.setPlatform("Booking.com");
        
        ReviewMessage.ReviewComment comment = new ReviewMessage.ReviewComment();
        comment.setHotelReviewId(987654321L);
        comment.setProviderId(334);
        comment.setRating(4.5);
        comment.setRatingText("Excellent");
        comment.setReviewTitle("Great stay at Test Hotel");
        comment.setReviewComments("This was an excellent hotel with great service and clean rooms.");
        comment.setReviewPositives("Clean rooms, friendly staff, good location");
        comment.setReviewNegatives("A bit expensive");
        comment.setCheckInDateMonthAndYear("October 2023");
        comment.setReviewDate("2023-10-18T06:23:54.380320+00:00");
        comment.setResponderName("Hotel Manager");
        comment.setResponseDateText("2023-10-20");
        comment.setResponseTranslateSource("Thank you for your feedback!");
        comment.setReviewProviderText("Booking.com");
        comment.setReviewProviderLogo("https://example.com/logo.png");
        comment.setEncryptedReviewData("encrypted_data_here");
        comment.setOriginalTitle("Great stay at Test Hotel");
        comment.setOriginalComment("This was an excellent hotel with great service and clean rooms.");
        
        // Add reviewer info
        ReviewMessage.ReviewerInfoDto reviewerInfo = new ReviewMessage.ReviewerInfoDto();
        reviewerInfo.setCountryId(1);
        reviewerInfo.setCountryName("United States");
        reviewerInfo.setFlagName("us");
        reviewerInfo.setReviewGroupId(1);
        reviewerInfo.setReviewGroupName("Couples");
        reviewerInfo.setRoomTypeId(1);
        reviewerInfo.setRoomTypeName("Standard Double Room");
        reviewerInfo.setLengthOfStay(2);
        reviewerInfo.setReviewerReviewedCount(5);
        reviewerInfo.setIsExpertReviewer(false);
        reviewerInfo.setIsShowGlobalIcon(true);
        reviewerInfo.setIsShowReviewedCount(true);
        comment.setReviewerInfo(reviewerInfo);
        
        message.setComment(comment);
        
        // Add overall provider scores
        ReviewMessage.OverallProvider overallProvider = new ReviewMessage.OverallProvider();
        overallProvider.setProviderId(334);
        overallProvider.setProvider("Booking.com");
        overallProvider.setOverallScore(4.3);
        overallProvider.setReviewCount(150);
        
        ReviewMessage.Grades grades = new ReviewMessage.Grades();
        grades.setCleanliness(4.5);
        grades.setFacilities(4.2);
        grades.setLocation(4.4);
        grades.setRoomComfortQuality(4.3);
        grades.setService(4.1);
        grades.setValueForMoney(4.0);
        overallProvider.setGrades(grades);
        
        message.setOverallByProviders(Arrays.asList(overallProvider));
        
        return message;
    }
    
    /**
     * Create a test ReviewMessage with bad review data
     */
    public static ReviewMessage createBadReviewMessage() {
        ReviewMessage message = new ReviewMessage();
        message.setHotelId(67890L);
        message.setHotelName("Bad Test Hotel");
        message.setPlatform("TripAdvisor");
        
        ReviewMessage.ReviewComment comment = new ReviewMessage.ReviewComment();
        comment.setHotelReviewId(111222333L);
        comment.setProviderId(456);
        comment.setRating(1.0);
        comment.setRatingText("Terrible");
        comment.setReviewTitle("Worst hotel experience ever");
        comment.setReviewComments("This hotel was absolutely terrible. Dirty rooms, rude staff, and terrible service.");
        comment.setReviewPositives("");
        comment.setReviewNegatives("Everything was bad");
        comment.setCheckInDateMonthAndYear("November 2023");
        comment.setReviewDate("2023-11-15T10:30:00.000000+00:00");
        comment.setResponderName(null);
        comment.setResponseDateText(null);
        comment.setResponseTranslateSource(null);
        comment.setReviewProviderText("TripAdvisor");
        comment.setReviewProviderLogo("https://example.com/tripadvisor-logo.png");
        comment.setEncryptedReviewData("encrypted_bad_data_here");
        comment.setOriginalTitle("Worst hotel experience ever");
        comment.setOriginalComment("This hotel was absolutely terrible. Dirty rooms, rude staff, and terrible service.");
        
        message.setComment(comment);
        
        return message;
    }
    
    /**
     * Create a test ReviewEntity
     */
    public static ReviewEntity createTestEntity() {
        ReviewEntity entity = new ReviewEntity();
        entity.setEntityId(12345);
        entity.setEntityName("Test Hotel");
        entity.setEntityType(EntityType.HOTEL);
        return entity;
    }
    
    /**
     * Create a test EntityReview
     */
    public static EntityReview createTestEntityReview() {
        EntityReview review = new EntityReview();
        
        EntityReview.EntityReviewId reviewId = new EntityReview.EntityReviewId();
        reviewId.setReviewId(987654321L);
        reviewId.setProviderId(334);
        review.setId(reviewId);
        
        review.setEntityId(12345);
        review.setPlatform("Booking.com");
        review.setRating(4.5);
        review.setRatingText("Excellent");
        review.setReviewTitle("Great stay at Test Hotel");
        review.setReviewComments("This was an excellent hotel with great service and clean rooms.");
        review.setReviewPositives("Clean rooms, friendly staff, good location");
        review.setReviewNegatives("A bit expensive");
        review.setCheckInDate("October 2023");
        review.setReviewDate(LocalDateTime.now());
        review.setResponderName("Hotel Manager");
        review.setResponseDate("2023-10-20");
        review.setResponseText("Thank you for your feedback!");
        review.setReviewProviderText("Booking.com");
        review.setReviewProviderLogo("https://example.com/logo.png");
        review.setEncryptedReviewData("encrypted_data_here");
        review.setOriginalTitle("Great stay at Test Hotel");
        review.setOriginalComment("This was an excellent hotel with great service and clean rooms.");
        
        return review;
    }
    
    /**
     * Create a test ReviewerInfo
     */
    public static ReviewerInfo createTestReviewerInfo() {
        ReviewerInfo reviewerInfo = new ReviewerInfo();
        
        ReviewerInfo.ReviewerInfoId infoId = new ReviewerInfo.ReviewerInfoId();
        infoId.setReviewId(987654321L);
        infoId.setProviderId(334);
        reviewerInfo.setId(infoId);
        
        reviewerInfo.setCountryId(1);
        reviewerInfo.setCountryName("United States");
        reviewerInfo.setFlagName("us");
        reviewerInfo.setReviewGroupId(1);
        reviewerInfo.setReviewGroupName("Couples");
        reviewerInfo.setRoomTypeId(1);
        reviewerInfo.setRoomTypeName("Standard Double Room");
        reviewerInfo.setLengthOfStay(2);
        reviewerInfo.setReviewerReviewedCount(5);
        reviewerInfo.setIsExpertReviewer(false);
        reviewerInfo.setIsShowGlobalIcon(true);
        reviewerInfo.setIsShowReviewedCount(true);
        
        return reviewerInfo;
    }
    
    /**
     * Create a test OverallProviderScore
     */
    public static OverallProviderScore createTestOverallProviderScore() {
        OverallProviderScore score = new OverallProviderScore();
        
        OverallProviderScore.OverallProviderScoreId scoreId = new OverallProviderScore.OverallProviderScoreId();
        scoreId.setProviderId(334);
        scoreId.setReviewId(987654321L);
        score.setId(scoreId);
        
        score.setEntityId(12345);
        score.setProvider("Booking.com");
        score.setOverallScore(4.3);
        score.setReviewCount(150);
        score.setCleanliness(4.5);
        score.setFacilities(4.2);
        score.setLocation(4.4);
        score.setRoomComfortQuality(4.3);
        score.setService(4.1);
        score.setValueForMoney(4.0);
        
        return score;
    }
    
    /**
     * Create multiple test ReviewMessages
     */
    public static List<ReviewMessage> createMultipleTestReviewMessages() {
        ReviewMessage message1 = createTestReviewMessage();
        
        ReviewMessage message2 = createTestReviewMessage();
        message2.setHotelId(54321L);
        message2.setHotelName("Another Test Hotel");
        message2.getComment().setHotelReviewId(555666777L);
        message2.getComment().setRating(3.5);
        message2.getComment().setReviewTitle("Decent hotel");
        message2.getComment().setReviewComments("It was okay, nothing special.");
        
        ReviewMessage message3 = createTestReviewMessage();
        message3.setHotelId(99999L);
        message3.setHotelName("Luxury Test Hotel");
        message3.getComment().setHotelReviewId(888999000L);
        message3.getComment().setRating(5.0);
        message3.getComment().setReviewTitle("Absolutely perfect!");
        message3.getComment().setReviewComments("This hotel exceeded all expectations. Perfect in every way.");
        
        return Arrays.asList(message1, message2, message3);
    }
} 