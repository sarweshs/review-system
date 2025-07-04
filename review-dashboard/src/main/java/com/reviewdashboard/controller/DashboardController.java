package com.reviewdashboard.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestTemplate;

import java.util.stream.Collectors;

@Controller
public class DashboardController {

    @Autowired
    private RestTemplate restTemplate;
    private static final String SERVICE_URL = "http://localhost:7070";

    @GetMapping("/post-login")
    public String postLogin(@AuthenticationPrincipal OidcUser oidcUser) {
        // Debug print all authorities
        oidcUser.getAuthorities().forEach(auth ->
                System.out.println("Authority: " + auth.getAuthority()));

        if (oidcUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_admin"))) {
            return "redirect:/admin";
        } else {
            return "redirect:/user";
        }
    }

    @GetMapping("/admin")
    public String adminPage(@AuthenticationPrincipal OidcUser oidcUser, Model model) {
        model.addAttribute("username", oidcUser.getPreferredUsername());
        model.addAttribute("roles", oidcUser.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.toList()));
        // Fetch sources from backend
        try {
            com.reviewcore.model.ReviewSource[] sources = restTemplate.getForObject(SERVICE_URL + "/api/sources", com.reviewcore.model.ReviewSource[].class);
            model.addAttribute("sources", sources != null ? java.util.List.of(sources) : java.util.List.of());
        } catch (Exception e) {
            model.addAttribute("sources", java.util.List.of());
        }
        return "admin";
    }

    @GetMapping("/user")
    public String userPage(@AuthenticationPrincipal OidcUser oidcUser, Model model) {
        model.addAttribute("username", oidcUser.getPreferredUsername());
        model.addAttribute("roles", oidcUser.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.toList()));
        return "user";
    }
}