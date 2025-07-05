package com.reviewdashboard.config;

import lombok.extern.slf4j.Slf4j;
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

@Slf4j
public class CustomOidcUserService extends OidcUserService {

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        // Debug: Print all claims
        log.debug("=== OIDC User Claims Debug ===");
        oidcUser.getClaims().forEach((key, value) -> {
            log.debug("Claim: {} = {}", key, value);
        });

        // Extract roles from token
        Set<GrantedAuthority> authorities = new HashSet<>();

        // Add realm roles
        if (oidcUser.getClaim("realm_access") != null) {
            Map<String, Object> realmAccess = oidcUser.getClaim("realm_access");
            log.debug("Realm access: {}", realmAccess);
            Collection<String> realmRoles = (Collection<String>) realmAccess.get("roles");
            if (realmRoles != null) {
                log.debug("Realm roles found: {}", realmRoles);
                authorities.addAll(realmRoles.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .collect(Collectors.toSet()));
            } else {
                log.debug("No realm roles found in realm_access");
            }
        } else {
            log.debug("No realm_access claim found");
        }

        // Try alternative role claims
        if (oidcUser.getClaim("roles") != null) {
            Collection<String> roles = (Collection<String>) oidcUser.getClaim("roles");
            log.debug("Direct roles claim found: {}", roles);
            authorities.addAll(roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toSet()));
        }

        // Add other standard claims as authorities if needed
        authorities.add(new SimpleGrantedAuthority("SCOPE_openid"));
        authorities.add(new SimpleGrantedAuthority("SCOPE_profile"));
        authorities.add(new SimpleGrantedAuthority("SCOPE_email"));

        log.debug("Final authorities: {}", authorities);
        log.debug("=== End Debug ===");

        return new DefaultOidcUser(authorities, oidcUser.getIdToken(), oidcUser.getUserInfo(), "preferred_username");
    }
}