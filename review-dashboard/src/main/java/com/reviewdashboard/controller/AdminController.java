package com.reviewdashboard.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Controller
@RequestMapping("/admin")
public class AdminController {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    
    @Autowired
    private RestTemplate restTemplate;
    
    private static final String SERVICE_URL = "http://localhost:7070";
    
    @GetMapping("/source/add")
    public String showAddSourceForm(Model model) {
        logger.info("Rendering add source form");
        // Get existing sources
        try {
            com.reviewcore.model.ReviewSource[] sources = restTemplate.getForObject(SERVICE_URL + "/api/sources", com.reviewcore.model.ReviewSource[].class);
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
            restTemplate.postForObject(SERVICE_URL + "/api/sources", request, com.reviewcore.model.ReviewSource.class);
            logger.info("Source added successfully");
            model.addAttribute("message", "Source added successfully!");
        } catch (Exception e) {
            logger.error("Failed to add source", e);
            model.addAttribute("error", "Failed to add source: " + e.getMessage());
        }
        // Refresh sources list
        try {
            com.reviewcore.model.ReviewSource[] sources = restTemplate.getForObject(SERVICE_URL + "/api/sources", com.reviewcore.model.ReviewSource[].class);
            model.addAttribute("sources", sources != null ? List.of(sources) : List.of());
        } catch (Exception e) {
            model.addAttribute("sources", List.of());
        }
        return "admin";
    }
    
    @GetMapping("/reviews")
    public String showReviews(Model model) {
        try {
            Object[] reviews = restTemplate.getForObject(SERVICE_URL + "/api/reviews", Object[].class);
            model.addAttribute("reviews", reviews != null ? List.of(reviews) : List.of());
        } catch (Exception e) {
            model.addAttribute("reviews", List.of());
        }
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
            restTemplate.postForObject(SERVICE_URL + "/api/sources/admin/source/update-active", form, Void.class);
            logger.info("Source status updated");
            model.addAttribute("message", "Source status updated!");
        } catch (Exception e) {
            logger.error("Failed to update source status", e);
            model.addAttribute("error", "Failed to update source status: " + e.getMessage());
        }
        return "redirect:/admin";
    }

    @PostMapping("/source/delete")
    public String deleteSource(@RequestParam("id") Long id, Model model) {
        logger.info("Received request to delete source with id={}", id);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("id", id.toString());
        try {
            restTemplate.postForObject(SERVICE_URL + "/api/sources/admin/source/delete", form, Void.class);
            logger.info("Source deleted with id={}", id);
            model.addAttribute("message", "Source deleted!");
        } catch (Exception e) {
            logger.error("Failed to delete source with id={}", id, e);
            model.addAttribute("error", "Failed to delete source: " + e.getMessage());
        }
        return "redirect:/admin";
    }
} 