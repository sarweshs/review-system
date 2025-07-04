package com.reviewdashboard.config;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CustomOidcUserService extends OidcUserService {

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        // Debug: Print all claims
        System.out.println("=== OIDC User Claims Debug ===");
        oidcUser.getClaims().forEach((key, value) -> {
            System.out.println("Claim: " + key + " = " + value);
        });

        // Extract roles from token
        Set<GrantedAuthority> authorities = new HashSet<>();

        // Add realm roles
        if (oidcUser.getClaim("realm_access") != null) {
            Map<String, Object> realmAccess = oidcUser.getClaim("realm_access");
            System.out.println("Realm access: " + realmAccess);
            Collection<String> realmRoles = (Collection<String>) realmAccess.get("roles");
            if (realmRoles != null) {
                System.out.println("Realm roles found: " + realmRoles);
                authorities.addAll(realmRoles.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .collect(Collectors.toSet()));
            } else {
                System.out.println("No realm roles found in realm_access");
            }
        } else {
            System.out.println("No realm_access claim found");
        }

        // Try alternative role claims
        if (oidcUser.getClaim("roles") != null) {
            Collection<String> roles = (Collection<String>) oidcUser.getClaim("roles");
            System.out.println("Direct roles claim found: " + roles);
            authorities.addAll(roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toSet()));
        }

        // Add other standard claims as authorities if needed
        authorities.add(new SimpleGrantedAuthority("SCOPE_openid"));
        authorities.add(new SimpleGrantedAuthority("SCOPE_profile"));
        authorities.add(new SimpleGrantedAuthority("SCOPE_email"));

        System.out.println("Final authorities: " + authorities);
        System.out.println("=== End Debug ===");

        return new DefaultOidcUser(authorities, oidcUser.getIdToken(), oidcUser.getUserInfo(), "preferred_username");
    }
}