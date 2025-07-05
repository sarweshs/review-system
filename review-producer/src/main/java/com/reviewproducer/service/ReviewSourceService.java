package com.reviewproducer.service;

import com.reviewcore.model.ReviewSource;
import com.reviewproducer.repository.ReviewSourceRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Value;

@Service
public class ReviewSourceService {
    private final ReviewSourceRepository reviewSourceRepository;

    @Value("${review-source-job.enabled:true}")
    private boolean jobEnabled;

    public ReviewSourceService(ReviewSourceRepository reviewSourceRepository) {
        this.reviewSourceRepository = reviewSourceRepository;
    }

    public List<ReviewSource> getActiveSources() {
        return reviewSourceRepository.findAllActive();
    }

    @Scheduled(fixedDelayString = "${review-source-job.fixed-delay-ms:60000}")
    public void scheduledPrintActiveSources() {
        if (!jobEnabled) return;
        List<ReviewSource> sources = getActiveSources();
        System.out.println("Active review sources: " + sources);
    }
} 