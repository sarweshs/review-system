package com.reviewdashboard.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

@Configuration
@EnableMethodSecurity
@Profile("!dev")
public class SecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http,
                                                        ClientRegistrationRepository clientRegistrationRepository) throws Exception {
        http
                .securityMatcher("/admin/**")
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().hasRole("admin")
                )
                .oauth2Login(oauth2 -> oauth2
                        .defaultSuccessUrl("/admin", true)
                        .userInfoEndpoint(userInfo -> userInfo
                                .oidcUserService(customOidcUserService())
                        )
                )
                .logout(logout -> logout
                        .logoutSuccessHandler(oidcLogoutSuccessHandler(clientRegistrationRepository))
                );

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain userSecurityFilterChain(HttpSecurity http,
                                                       ClientRegistrationRepository clientRegistrationRepository) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/error", "/webjars/**").permitAll()
                        .requestMatchers("/user").hasRole("user")
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .defaultSuccessUrl("/post-login", true)
                        .userInfoEndpoint(userInfo -> userInfo
                                .oidcUserService(customOidcUserService())
                        )
                )
                .logout(logout -> logout
                        .logoutSuccessHandler(oidcLogoutSuccessHandler(clientRegistrationRepository))
                );

        return http.build();
    }

    @Bean
    public CustomOidcUserService customOidcUserService() {
        return new CustomOidcUserService();
    }

    @Bean
    public LogoutSuccessHandler oidcLogoutSuccessHandler(
            ClientRegistrationRepository clientRegistrationRepository) {
        OidcClientInitiatedLogoutSuccessHandler successHandler =
                new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
        successHandler.setPostLogoutRedirectUri("{baseUrl}");
        return successHandler;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");
        grantedAuthoritiesConverter.setAuthoritiesClaimName("realm_access.roles");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }
}