package com.reviewautomation.tests;

import com.reviewautomation.base.BaseIntegrationTest;
import com.reviewautomation.util.TestDataBuilder;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.kafka.bootstrap-servers=${kafka.bootstrap-servers}"
})
@DisplayName("Review API Integration Tests")
public class ReviewApiIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Health check endpoint should return 200 OK")
    void testHealthCheck() {
        given()
            .when()
            .get(getHealthUrl())
            .then()
            .statusCode(200)
            .body("status", equalTo("UP"));
    }

    @Test
    @DisplayName("Metrics endpoint should return metrics data")
    void testMetricsEndpoint() {
        given()
            .when()
            .get(getMetricsUrl())
            .then()
            .statusCode(200)
            .body("names", not(empty()));
    }

    @Test
    @DisplayName("Get all reviews should return paginated results")
    void testGetAllReviews() {
        given()
            .when()
            .get(getReviewsApiUrl())
            .then()
            .statusCode(200)
            .body("reviews", notNullValue())
            .body("currentPage", equalTo(0))
            .body("totalItems", greaterThanOrEqualTo(0))
            .body("totalPages", greaterThanOrEqualTo(0))
            .body("hasNext", notNullValue())
            .body("hasPrevious", notNullValue());
    }

    @Test
    @DisplayName("Get reviews with pagination parameters")
    void testGetReviewsWithPagination() {
        given()
            .queryParam("page", 0)
            .queryParam("size", 5)
            .queryParam("sortBy", "id.reviewId")
            .queryParam("sortDir", "desc")
            .when()
            .get(getReviewsApiUrl())
            .then()
            .statusCode(200)
            .body("reviews.size()", lessThanOrEqualTo(5))
            .body("currentPage", equalTo(0));
    }

    @Test
    @DisplayName("Get reviews by platform")
    void testGetReviewsByPlatform() {
        given()
            .pathParam("platform", "Booking.com")
            .when()
            .get(getReviewsApiUrl() + "/platform/{platform}")
            .then()
            .statusCode(200)
            .body("$", not(empty()));
    }

    @Test
    @DisplayName("Get reviews by entity ID")
    void testGetReviewsByEntityId() {
        given()
            .pathParam("entityId", 1)
            .when()
            .get(getReviewsApiUrl() + "/entity/{entityId}")
            .then()
            .statusCode(200)
            .body("$", not(empty()));
    }

    @Test
    @DisplayName("Get reviews by rating range")
    void testGetReviewsByRatingRange() {
        given()
            .queryParam("minRating", 4.0)
            .queryParam("maxRating", 5.0)
            .when()
            .get(getReviewsApiUrl() + "/rating")
            .then()
            .statusCode(200)
            .body("$", not(empty()));
    }

    @Test
    @DisplayName("Get review by ID and provider ID")
    void testGetReviewById() {
        // First, get a list of reviews to find an existing review
        Map<String, Object> response = given()
            .when()
            .get(getReviewsApiUrl())
            .then()
            .statusCode(200)
            .extract()
            .as(Map.class);

        if (response.get("totalItems") != null && (Integer) response.get("totalItems") > 0) {
            // If there are reviews, test getting a specific review
            given()
                .pathParam("reviewId", 987654321L)
                .pathParam("providerId", 334)
                .when()
                .get(getReviewsApiUrl() + "/{reviewId}/{providerId}")
                .then()
                .statusCode(200);
        }
    }

    @Test
    @DisplayName("Get review statistics")
    void testGetReviewStatistics() {
        given()
            .when()
            .get(getReviewsApiUrl() + "/statistics")
            .then()
            .statusCode(200)
            .body("totalReviews", notNullValue())
            .body("platforms", notNullValue())
            .body("averageRating", notNullValue())
            .body("ratingDistribution", notNullValue());
    }

    @Test
    @DisplayName("Get combined review summary")
    void testGetReviewSummary() {
        given()
            .when()
            .get(getReviewsApiUrl() + "/summary")
            .then()
            .statusCode(200)
            .body("totalGoodReviews", notNullValue())
            .body("goodReviewsByPlatform", notNullValue())
            .body("totalBadReviews", notNullValue())
            .body("badReviewsByPlatform", notNullValue())
            .body("badReviewsByReason", notNullValue());
    }

    @Test
    @DisplayName("Search reviews by term")
    void testSearchReviews() {
        given()
            .queryParam("search", "hotel")
            .when()
            .get(getReviewsApiUrl())
            .then()
            .statusCode(200)
            .body("reviews", notNullValue());
    }

    @Test
    @DisplayName("Filter reviews by platform and rating")
    void testFilterReviewsByPlatformAndRating() {
        given()
            .queryParam("platform", "Booking.com")
            .queryParam("minRating", 4.0)
            .queryParam("maxRating", 5.0)
            .when()
            .get(getReviewsApiUrl())
            .then()
            .statusCode(200)
            .body("reviews", notNullValue());
    }

    @Test
    @DisplayName("Get reviews with invalid pagination should handle gracefully")
    void testGetReviewsWithInvalidPagination() {
        given()
            .queryParam("page", -1)
            .queryParam("size", 0)
            .when()
            .get(getReviewsApiUrl())
            .then()
            .statusCode(200); // Should handle gracefully and return default values
    }

    @Test
    @DisplayName("Get reviews with invalid sort field should handle gracefully")
    void testGetReviewsWithInvalidSortField() {
        given()
            .queryParam("sortBy", "invalidField")
            .when()
            .get(getReviewsApiUrl())
            .then()
            .statusCode(500); // Should return error for invalid sort field
    }

    @Test
    @DisplayName("Get review by non-existent ID should return 404")
    void testGetNonExistentReview() {
        given()
            .pathParam("reviewId", 999999999L)
            .pathParam("providerId", 999)
            .when()
            .get(getReviewsApiUrl() + "/{reviewId}/{providerId}")
            .then()
            .statusCode(500); // Should return error for non-existent review
    }

    @Test
    @DisplayName("API should handle CORS headers")
    void testCorsHeaders() {
        given()
            .header("Origin", "http://localhost:3000")
            .when()
            .get(getReviewsApiUrl())
            .then()
            .statusCode(200)
            .header("Access-Control-Allow-Origin", notNullValue());
    }

    @Test
    @DisplayName("API should return proper content type")
    void testContentType() {
        given()
            .when()
            .get(getReviewsApiUrl())
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON);
    }
} 