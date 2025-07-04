package com.reviewservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan("com.reviewcore.model")  // Explicitly scan entity package
@EnableJpaRepositories("com.reviewservice.repository")  // Explicitly scan repository package
public class ReviewSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReviewSystemApplication.class, args);
    }

} 