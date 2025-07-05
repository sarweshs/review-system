package com.reviewconsumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EnableKafka
@EntityScan(basePackages = {"com.reviewcore.model"})
public class ReviewConsumerApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ReviewConsumerApplication.class, args);
    }
} 