package com.reviewconsumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class ReviewConsumerApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ReviewConsumerApplication.class, args);
    }
} 