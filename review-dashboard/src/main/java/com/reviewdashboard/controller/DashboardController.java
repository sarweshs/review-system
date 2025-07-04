package com.reviewdashboard.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class DashboardController {

    @GetMapping("/post-login")
    public String postLogin(@AuthenticationPrincipal OidcUser oidcUser) {
        oidcUser.getAuthorities().forEach(auth -> System.out.println("Authority: " + auth.getAuthority()));

        if (oidcUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_admin"))) {
            return "redirect:/admin";
        } else {
            return "redirect:/user";
        }
    }

    @GetMapping("/admin")
    public String adminPage(@AuthenticationPrincipal OidcUser oidcUser, Model model) {
        model.addAttribute("username", oidcUser.getPreferredUsername());
        return "admin";
    }

    @GetMapping("/user")
    public String userPage(@AuthenticationPrincipal OidcUser oidcUser, Model model) {
        model.addAttribute("username", oidcUser.getPreferredUsername());
        return "user";
    }
}
