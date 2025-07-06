package com.reviewdashboard.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestTemplate;

import java.util.stream.Collectors;

@Slf4j
@Controller
public class DashboardController {

    @Autowired
    private RestTemplate restTemplate;
    
    @org.springframework.beans.factory.annotation.Value("${review.service.url:http://review-service:7070}")
    private String serviceUrl;

    @GetMapping("/post-login")
    public String postLogin(@AuthenticationPrincipal OidcUser oidcUser) {
        // Debug print all authorities
        oidcUser.getAuthorities().forEach(auth ->
                log.debug("Authority: {}", auth.getAuthority()));

        if (oidcUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_admin"))) {
            log.info("User {} has admin role, redirecting to admin page", oidcUser.getPreferredUsername());
            return "redirect:/admin";
        } else {
            log.info("User {} has user role, redirecting to user page", oidcUser.getPreferredUsername());
            return "redirect:/user";
        }
    }

    @GetMapping("/admin")
    public String adminPage(@AuthenticationPrincipal OidcUser oidcUser, Model model) {
        log.debug("Admin page requested by user: {}", oidcUser.getPreferredUsername());
        
        model.addAttribute("username", oidcUser.getPreferredUsername());
        model.addAttribute("roles", oidcUser.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.toList()));
        
        // Add service URL to model for frontend JavaScript
        model.addAttribute("serviceUrl", serviceUrl);
        
        // Fetch sources from backend
        try {
            com.reviewcore.model.ReviewSource[] sources = restTemplate.getForObject(serviceUrl + "/api/sources", com.reviewcore.model.ReviewSource[].class);
            model.addAttribute("sources", sources != null ? java.util.List.of(sources) : java.util.List.of());
            log.debug("Successfully fetched {} sources for admin page", sources != null ? sources.length : 0);
        } catch (Exception e) {
            log.error("Failed to fetch sources from backend service", e);
            model.addAttribute("sources", java.util.List.of());
        }
        return "admin";
    }

    @GetMapping("/user")
    public String userPage(@AuthenticationPrincipal OidcUser oidcUser, Model model) {
        log.debug("User page requested by user: {}", oidcUser.getPreferredUsername());
        
        model.addAttribute("username", oidcUser.getPreferredUsername());
        model.addAttribute("roles", oidcUser.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.toList()));
        
        // Add service URL to model for frontend JavaScript
        model.addAttribute("serviceUrl", serviceUrl);
        
        return "user";
    }
}