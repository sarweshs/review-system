package com.reviewproducer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReviewValidationServiceTest {
    
    private ReviewValidationService validationService;
    
    @BeforeEach
    void setUp() {
        validationService = new ReviewValidationService(new ObjectMapper());
    }
    
    @Test
    void testValidReview() {
        String validReview = """
            {
                "hotel_id": "12345",
                "hotel_name": "Test Hotel",
                "platform": "Booking.com",
                "rating": 4.5,
                "review_text": "Great hotel!"
            }
            """;
        
        ReviewValidationService.ValidationResult result = validationService.validateReview(validReview);
        assertTrue(result.isValid());
        assertNull(result.getReason());
    }
    
    @Test
    void testNullHotelId() {
        String invalidReview = """
            {
                "hotel_id": null,
                "hotel_name": "Test Hotel",
                "platform": "Agoda",
                "rating": 4.0
            }
            """;
        
        ReviewValidationService.ValidationResult result = validationService.validateReview(invalidReview);
        assertFalse(result.isValid());
        assertEquals("HOTEL_ID_NULL", result.getReason());
    }
    
    @Test
    void testNullHotelName() {
        String invalidReview = """
            {
                "hotel_id": "12345",
                "hotel_name": null,
                "platform": "Expedia",
                "rating": 3.5
            }
            """;
        
        ReviewValidationService.ValidationResult result = validationService.validateReview(invalidReview);
        assertFalse(result.isValid());
        assertEquals("HOTEL_NAME_NULL", result.getReason());
    }
    
    @Test
    void testMissingHotelId() {
        String invalidReview = """
            {
                "hotel_name": "Test Hotel",
                "platform": "Booking.com",
                "rating": 4.5
            }
            """;
        
        ReviewValidationService.ValidationResult result = validationService.validateReview(invalidReview);
        assertFalse(result.isValid());
        assertEquals("HOTEL_ID_NULL", result.getReason());
    }
    
    @Test
    void testMissingHotelName() {
        String invalidReview = """
            {
                "hotel_id": "12345",
                "platform": "Agoda",
                "rating": 4.0
            }
            """;
        
        ReviewValidationService.ValidationResult result = validationService.validateReview(invalidReview);
        assertFalse(result.isValid());
        assertEquals("HOTEL_NAME_NULL", result.getReason());
    }
    
    @Test
    void testInvalidJson() {
        String invalidJson = "{ invalid json }";
        
        ReviewValidationService.ValidationResult result = validationService.validateReview(invalidJson);
        assertFalse(result.isValid());
        assertTrue(result.getReason().startsWith("INVALID_JSON:"));
    }
    
    @Test
    void testExtractPlatform() {
        String reviewWithPlatform = """
            {
                "hotel_id": "12345",
                "hotel_name": "Test Hotel",
                "platform": "Booking.com",
                "rating": 4.5
            }
            """;
        
        String platform = validationService.extractPlatform(reviewWithPlatform);
        assertEquals("Booking.com", platform);
    }
    
    @Test
    void testExtractPlatformNull() {
        String reviewWithoutPlatform = """
            {
                "hotel_id": "12345",
                "hotel_name": "Test Hotel",
                "rating": 4.5
            }
            """;
        
        String platform = validationService.extractPlatform(reviewWithoutPlatform);
        assertEquals("unknown", platform);
    }
} 