package com.reviewservice.service;

import com.reviewcore.model.Review;
import com.reviewservice.repository.ReviewRepository;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class ReviewServiceTest {
    @Test
    void testProcessReview_savesIfNotExists() {
        ReviewRepository repo = mock(ReviewRepository.class);
        ReviewService service = new ReviewService(repo);

        Review review = new Review();
        review.setReviewId("abc123");

        when(repo.existsByReviewId("abc123")).thenReturn(false);

        service.processReview(review);

        verify(repo, times(1)).save(review);
    }
} 