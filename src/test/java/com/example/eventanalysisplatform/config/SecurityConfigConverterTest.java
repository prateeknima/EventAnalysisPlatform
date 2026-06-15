package com.example.eventanalysisplatform.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigConverterTest {

    private final SecurityConfig securityConfig = new SecurityConfig();

    @Test
    void convertsKeycloakClientRolesToSpringAuthorities() {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .issuer("http://localhost:8081/realms/event-analysis")
                .subject("service-account")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .claim("resource_access", Map.of(
                        "event-analysis-api", Map.of(
                                "roles", List.of(
                                        "incidents.read",
                                        "incidents.write",
                                        "metrics.read"
                                )
                        )
                ))
                .build();

        AbstractAuthenticationToken authentication =
                securityConfig.keycloakJwtAuthenticationConverter().convert(jwt);

        assertThat(authentication).isNotNull();

        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder(
                        "incidents.read",
                        "incidents.write",
                        "metrics.read"
                );
    }

    @Test
    void returnsNoAuthoritiesWhenClientRolesAreMissing() {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .issuer("http://localhost:8081/realms/event-analysis")
                .subject("service-account")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .claim("resource_access", Map.of(
                        "different-client", Map.of(
                                "roles", List.of("incidents.read")
                        )
                ))
                .build();

        AbstractAuthenticationToken authentication =
                securityConfig.keycloakJwtAuthenticationConverter().convert(jwt);

        assertThat(authentication).isNotNull();

        assertThat(authentication.getAuthorities())
                .isEmpty();
    }
}