package com.reviewautomation.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.reviewautomation.config.TestConfig;
import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import com.reviewservice.ReviewSystemApplication;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;

@SpringBootTest(classes = com.reviewservice.ReviewSystemApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    protected static final Logger log = LoggerFactory.getLogger(BaseIntegrationTest.class);
    
    @LocalServerPort
    protected int port;
    
    protected static ObjectMapper objectMapper;
    protected static RestAssuredConfig restAssuredConfig;
    
    @BeforeAll
    static void setUp() {
        // Configure ObjectMapper
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        // Configure RestAssured
        restAssuredConfig = RestAssuredConfig.config()
                .objectMapperConfig(ObjectMapperConfig.objectMapperConfig().jackson2ObjectMapperFactory((cls, charset) -> objectMapper));
        
        RestAssured.config = restAssuredConfig;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        
        log.info("Test environment initialized");
        log.info("Database URL: {}", TestConfig.getDatabaseUrl());
        log.info("Kafka Bootstrap Servers: {}", TestConfig.getKafkaBootstrapServers());
    }
    
    @BeforeEach
    void setUpTest() {
        RestAssured.port = port;
        log.info("Test server running on port: {}", port);
        
        // Wait for services to be ready
        await().atMost(30, TimeUnit.SECONDS).until(this::isServiceReady);
    }
    
    @AfterAll
    static void tearDown() {
        log.info("Cleaning up test environment");
        // Cleanup will be handled by TestContainers
    }
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Database properties
        registry.add("spring.datasource.url", TestConfig::getDatabaseUrl);
        registry.add("spring.datasource.username", TestConfig::getDatabaseUsername);
        registry.add("spring.datasource.password", TestConfig::getDatabasePassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        
        // JPA properties
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "true");
        registry.add("spring.jpa.properties.hibernate.format_sql", () -> "true");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
        
        // Kafka properties
        registry.add("spring.kafka.bootstrap-servers", TestConfig::getKafkaBootstrapServers);
        registry.add("kafka.bootstrap-servers", TestConfig::getKafkaBootstrapServers);
        
        // Application specific properties
        registry.add("app.kafka.consumer.group-id", () -> "test-consumer-group");
        registry.add("app.kafka.producer.client-id", () -> "test-producer");
        
        // Server properties
        registry.add("server.port", () -> "0"); // Random port
    }
    
    /**
     * Check if the service is ready to accept requests
     */
    protected boolean isServiceReady() {
        try {
            given()
                .when()
                .get("/actuator/health")
                .then()
                .statusCode(200);
            return true;
        } catch (Exception e) {
            log.debug("Service not ready yet: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get database connection for direct database operations
     */
    protected Connection getDatabaseConnection() throws SQLException {
        return DriverManager.getConnection(
            TestConfig.getDatabaseUrl(),
            TestConfig.getDatabaseUsername(),
            TestConfig.getDatabasePassword()
        );
    }
    
    /**
     * Wait for a condition to be true with timeout
     */
    protected void waitForCondition(java.util.concurrent.Callable<Boolean> condition, long timeoutSeconds) {
        await()
            .atMost(timeoutSeconds, TimeUnit.SECONDS)
            .until(condition);
    }
    
    /**
     * Wait for a condition to be true with default timeout
     */
    protected void waitForCondition(java.util.concurrent.Callable<Boolean> condition) {
        waitForCondition(condition, 30);
    }
    
    /**
     * Get the base URL for API requests
     */
    protected String getBaseUrl() {
        return "http://localhost:" + port;
    }
    
    /**
     * Get the API base URL
     */
    protected String getApiBaseUrl() {
        return getBaseUrl() + "/api";
    }
    
    /**
     * Get the reviews API URL
     */
    protected String getReviewsApiUrl() {
        return getApiBaseUrl() + "/reviews";
    }
    
    /**
     * Get the health check URL
     */
    protected String getHealthUrl() {
        return getBaseUrl() + "/actuator/health";
    }
    
    /**
     * Get the metrics URL
     */
    protected String getMetricsUrl() {
        return getBaseUrl() + "/actuator/metrics";
    }
} 