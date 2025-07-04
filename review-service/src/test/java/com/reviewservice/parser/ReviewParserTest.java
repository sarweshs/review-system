package com.reviewservice.parser;

import com.reviewcore.model.Review;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReviewParserTest {
    @Test
    void testParse_validJson() throws Exception {
        ReviewParser parser = new ReviewParser();
        String json = "{\"reviewId\":\"abc123\",\"provider\":\"Agoda\"}";
        Review review = parser.parse(json);
        assertEquals("abc123", review.getReviewId());
        assertEquals("Agoda", review.getProvider());
    }
} 