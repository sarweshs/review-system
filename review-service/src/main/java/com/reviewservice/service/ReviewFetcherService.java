package com.reviewservice.service;

import com.reviewservice.kafka.ReviewProducer;
import com.reviewservice.parser.ReviewParser;
import com.reviewservice.storage.StorageService;
import com.reviewservice.repository.ReviewSourceRepository;
import com.reviewcore.model.ReviewSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ReviewFetcherService {
    @Autowired
    private Map<String, StorageService> storageServices;
    @Autowired
    private ReviewProducer reviewProducer;
    @Autowired
    private ReviewParser reviewParser;
    @Autowired
    private ReviewSourceRepository reviewSourceRepository;

    @Scheduled(fixedDelayString = "${fetcher.poll-interval-ms:60000}")
    public void fetchAndProcessReviews() {
        List<ReviewSource> sources = reviewSourceRepository.findAll();
        for (ReviewSource source : sources) {
            String backend = source.getBackend().toLowerCase();
            StorageService storageService = storageServices.get(backend + "StorageService");
            if (storageService == null) {
                // Log or handle missing storage service
                continue;
            }
            // TODO: Implement logic to process files for this source using storageService
        }
    }
} 