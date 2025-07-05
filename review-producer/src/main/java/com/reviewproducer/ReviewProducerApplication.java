package com.reviewproducer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EntityScan(basePackages = {"com.reviewcore.model"})
@EnableJpaRepositories(basePackages = {"com.reviewproducer.repository"})
@EnableScheduling
public class ReviewProducerApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ReviewProducerApplication.class, args);
    }
} 