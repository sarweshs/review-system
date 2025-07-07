package com.reviewdashboard.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

@Controller
@RequestMapping("/admin")
public class AdminController {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${review.service.url:http://review-service:7070}")
    private String serviceUrl;
    
    @GetMapping("/source/add")
    public String showAddSourceForm(Model model) {
        logger.info("Rendering add source form");
        // Get existing sources
        try {
            com.reviewcore.model.ReviewSource[] sources = restTemplate.getForObject(serviceUrl + "/api/sources", com.reviewcore.model.ReviewSource[].class);
            model.addAttribute("sources", sources != null ? List.of(sources) : List.of());
        } catch (Exception e) {
            model.addAttribute("sources", List.of());
        }
        return "admin";
    }
    
    @PostMapping("/source/add")
    public String addSource(@RequestParam Map<String, String> params, Model model) {
        logger.info("Received request to add source");
        logger.debug("Request params: {}", params);
        // Forward all form fields to backend as a map
        Map<String, Object> request = new HashMap<>(params);
        try {
            restTemplate.postForObject(serviceUrl + "/api/sources", request, com.reviewcore.model.ReviewSource.class);
            logger.info("Source added successfully");
            return "redirect:/admin?message=Source added successfully!";
        } catch (Exception e) {
            logger.error("Failed to add source", e);
            return "redirect:/admin?error=Failed to add source: " + e.getMessage();
        }
    }
    
    @GetMapping("/reviews")
    public String showReviews(Model model) {
        // The reviews will be loaded via AJAX in the template
        return "admin";
    }

    @PostMapping("/source/update-active")
    public String updateActive(@RequestParam(value = "activeIds", required = false) List<Long> activeIds, Model model) {
        logger.info("Received request to update active status");
        logger.debug("Active IDs: {}", activeIds);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        if (activeIds != null) {
            for (Long id : activeIds) {
                form.add("activeIds", id.toString());
            }
        }
        try {
            ResponseEntity<Void> resp = restTemplate.postForEntity(serviceUrl + "/api/sources/admin/source/update-active", form, Void.class);
            if (resp.getStatusCode() == HttpStatus.NO_CONTENT) {
                logger.info("Source status updated");
                return "redirect:/admin?message=Source status updated successfully!";
            } else {
                logger.warn("Unexpected response status: {}", resp.getStatusCode());
                return "redirect:/admin?error=Failed to update source status: Unexpected response";
            }
        } catch (Exception e) {
            logger.error("Failed to update source status", e);
            return "redirect:/admin?error=Failed to update source status: " + e.getMessage();
        }
    }

    @PostMapping("/source/delete")
    public String deleteSource(@RequestParam("id") Long id, Model model) {
        logger.info("Received request to delete source with id={}", id);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("id", id.toString());
        try {
            ResponseEntity<Void> resp = restTemplate.postForEntity(serviceUrl + "/api/sources/admin/source/delete", form, Void.class);
            if (resp.getStatusCode() == HttpStatus.NO_CONTENT) {
                logger.info("Source deleted with id={}", id);
                // Use flash attributes for the message since we're redirecting
                return "redirect:/admin?message=Source deleted successfully!";
            } else {
                logger.warn("Unexpected response status: {}", resp.getStatusCode());
                return "redirect:/admin?error=Failed to delete source: Unexpected response";
            }
        } catch (Exception e) {
            logger.error("Failed to delete source with id={}", id, e);
            return "redirect:/admin?error=Failed to delete source: " + e.getMessage();
        }
    }
} 