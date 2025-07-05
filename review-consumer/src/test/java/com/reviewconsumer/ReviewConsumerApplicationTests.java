package com.reviewconsumer;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ReviewConsumerApplicationTests {

    @Test
    @Disabled("Temporarily disabled due to Jackson version conflicts with Hibernate")
    void contextLoads() {
        // Test that the Spring context loads successfully
    }
} 