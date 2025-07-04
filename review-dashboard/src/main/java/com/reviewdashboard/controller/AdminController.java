package com.reviewdashboard.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {
    
    @Autowired
    private RestTemplate restTemplate;
    
    private static final String SERVICE_URL = "http://localhost:7070";
    
    @GetMapping("/source/add")
    public String showAddSourceForm(Model model) {
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
        // Forward all form fields to backend as a map
        Map<String, Object> request = new HashMap<>(params);
        try {
            restTemplate.postForObject(SERVICE_URL + "/api/sources", request, com.reviewcore.model.ReviewSource.class);
            model.addAttribute("message", "Source added successfully!");
        } catch (Exception e) {
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
} 