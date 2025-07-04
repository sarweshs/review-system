package com.reviewservice.repository;

import com.reviewcore.model.Review;
import com.reviewservice.config.TestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = "spring.profiles.active=test")
@EntityScan(basePackages = "com.reviewcore.model")
@EnableJpaRepositories(basePackages = "com.reviewservice.repository")
@Import(TestConfig.class)
class ReviewRepositoryTest {
    @Autowired
    private ReviewRepository repo;

    @Test
    void testSaveAndFind() {
        Review review = new Review();
        review.setReviewId("test123");
        review.setProvider("Agoda");
        repo.save(review);

        assertTrue(repo.existsByReviewId("test123"));
    }
} 