package com.reviewservice.controller;

import com.reviewcore.model.*;
import com.reviewservice.repository.ReviewSourceRepository;
import com.reviewservice.service.CredentialService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/sources")
public class ReviewSourceController {
    private static final Logger logger = LoggerFactory.getLogger(ReviewSourceController.class);
    @Autowired
    private ReviewSourceRepository reviewSourceRepository;
    @Autowired
    private CredentialService credentialService;

    @PostMapping
    public ReviewSource addSource(@RequestBody Map<String, String> params) {
        logger.info("Received request to add source");
        logger.debug("Request params: {}", params);
        ReviewSource source = new ReviewSource();
        source.setName(params.get("name"));
        source.setUri(params.get("uri"));
        // Handle credential
        String credentialType = params.get("credentialType");
        if (credentialType != null && !credentialType.isEmpty()) {
            Credential credential = null;
            switch (credentialType) {
                case "basic":
                    credential = credentialService.createBasicCredential(params.get("username"), params.get("password"));
                    break;
                case "apikey":
                    credential = credentialService.createApiKeyCredential(params.get("apiKey"), params.get("headerName"));
                    break;
                case "oauth":
                    credential = credentialService.createOAuthCredential(
                        params.get("clientId"),
                        params.get("clientSecret"),
                        params.get("accessToken"),
                        params.get("refreshToken"),
                        params.get("tokenUrl")
                    );
                    break;
                case "aws":
                    credential = credentialService.createAwsCredential(
                        params.get("awsAccessKeyId"),
                        params.get("awsSecretAccessKey")
                    );
                    break;
            }
            if (credential != null && credentialService.validateCredential(credential)) {
                try {
                    String encryptedJson = credentialService.encryptCredential(credential);
                    source.setCredentialJson(encryptedJson);
                } catch (Exception e) {
                    logger.error("Failed to encrypt credential", e);
                    throw new RuntimeException("Failed to encrypt credential: " + e.getMessage(), e);
                }
            }
        } else {
            source.setCredentialJson(null);
        }
        return reviewSourceRepository.save(source);
    }

    @GetMapping
    public Iterable<ReviewSource> getSources() {
        logger.info("Fetching all review sources");
        return reviewSourceRepository.findAll();
    }

    @PostMapping("/admin/source/update-active")
    public String updateActive(@RequestParam(value = "activeIds", required = false) Long[] activeIds) {
        logger.info("Updating active status for sources");
        logger.debug("Active IDs: {}", (Object) activeIds);
        Iterable<ReviewSource> allSources = reviewSourceRepository.findAll();
        java.util.Set<Long> activeSet = activeIds != null ? java.util.Arrays.stream(activeIds).collect(java.util.stream.Collectors.toSet()) : java.util.Collections.emptySet();
        for (ReviewSource src : allSources) {
            boolean shouldBeActive = activeSet.contains(src.getId());
            if (src.getActive() == null || src.getActive() != shouldBeActive) {
                src.setActive(shouldBeActive);
                reviewSourceRepository.save(src);
            }
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/source/delete")
    public String deleteSource(@RequestParam("id") Long id) {
        logger.info("Deleting source with id={}", id);
        try {
            reviewSourceRepository.deleteById(id);
            logger.info("Successfully deleted source with id={}", id);
        } catch (Exception e) {
            logger.error("Failed to delete source with id={}", id, e);
            throw e;
        }
        return "redirect:/admin";
    }
} 