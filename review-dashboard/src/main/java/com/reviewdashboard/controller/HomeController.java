package com.reviewdashboard.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(@AuthenticationPrincipal OidcUser oidcUser) {
        if (oidcUser != null) {
            return "redirect:/post-login";
        }
        return "index";
    }
}