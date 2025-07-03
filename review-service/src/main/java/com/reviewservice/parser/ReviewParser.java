package com.reviewservice.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reviewcore.model.Review;
import org.springframework.stereotype.Component;

@Component
public class ReviewParser {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Review parse(String jsonLine) throws Exception {
        return objectMapper.readValue(jsonLine, Review.class);
    }
} 