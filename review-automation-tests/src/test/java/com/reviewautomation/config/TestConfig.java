package com.reviewautomation.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.util.Map;

@TestConfiguration
public class TestConfig {

    private static final PostgreSQLContainer<?> postgresContainer;
    private static final KafkaContainer kafkaContainer;

    static {
        postgresContainer = new PostgreSQLContainer<>("postgres:15-alpine")
                .withDatabaseName("review_test_db")
                .withUsername("test_user")
                .withPassword("test_password")
                .withInitScript("init-test-db.sql");
        
        kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"))
                .withKraft()
                .withReuse(true);
        
        postgresContainer.start();
        kafkaContainer.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Database properties
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", postgresContainer::getDriverClassName);
        
        // JPA properties
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "true");
        registry.add("spring.jpa.properties.hibernate.format_sql", () -> "true");
        
        // Kafka properties
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
        registry.add("kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
        
        // Application specific properties
        registry.add("app.kafka.consumer.group-id", () -> "test-consumer-group");
        registry.add("app.kafka.producer.client-id", () -> "test-producer");
    }

    public static String getKafkaBootstrapServers() {
        return kafkaContainer.getBootstrapServers();
    }

    public static String getDatabaseUrl() {
        return postgresContainer.getJdbcUrl();
    }

    public static String getDatabaseUsername() {
        return postgresContainer.getUsername();
    }

    public static String getDatabasePassword() {
        return postgresContainer.getPassword();
    }

    public static void stopContainers() {
        if (postgresContainer != null && postgresContainer.isRunning()) {
            postgresContainer.stop();
        }
        if (kafkaContainer != null && kafkaContainer.isRunning()) {
            kafkaContainer.stop();
        }
    }
} 