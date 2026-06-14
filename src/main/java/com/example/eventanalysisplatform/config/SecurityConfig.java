package com.example.eventanalysisplatform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Configuration
public class SecurityConfig {

    private static final String CLIENT_ID = "event-analysis-api";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/actuator/prometheus").hasAuthority("metrics.read")
                        .requestMatchers(HttpMethod.POST, "/incidents").hasAuthority("incidents.write")
                        .requestMatchers(HttpMethod.GET, "/incidents/**").hasAuthority("incidents.read")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakJwtAuthenticationConverter()))
                )
                .build();
    }

    Converter<Jwt, ? extends AbstractAuthenticationToken> keycloakJwtAuthenticationConverter() {
        return jwt -> {
            Map<String, Object> resourceAccess = jwt.getClaim("resource_access");

            if (resourceAccess == null || !resourceAccess.containsKey(CLIENT_ID)) {
                return new JwtAuthenticationToken(jwt, List.of());
            }

            Map<String, Object> clientAccess =
                    (Map<String, Object>) resourceAccess.get(CLIENT_ID);

            Collection<String> roles =
                    (Collection<String>) clientAccess.getOrDefault("roles", List.of());

            List<GrantedAuthority> authorities = roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .map(authority -> (GrantedAuthority) authority)
                    .toList();

            return new JwtAuthenticationToken(jwt, authorities);
        };
    }
}