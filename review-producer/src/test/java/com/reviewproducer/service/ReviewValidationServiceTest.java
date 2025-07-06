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
                "hotelId": "12345",
                "hotelName": "Test Hotel",
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
                "hotelId": null,
                "hotelName": "Test Hotel",
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
                "hotelId": "12345",
                "hotelName": null,
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
                "hotelName": "Test Hotel",
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
                "hotelId": "12345",
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
                "hotelId": "12345",
                "hotelName": "Test Hotel",
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
                "hotelId": "12345",
                "hotelName": "Test Hotel",
                "rating": 4.5
            }
            """;
        
        String platform = validationService.extractPlatform(reviewWithoutPlatform);
        assertEquals("unknown", platform);
    }
} 