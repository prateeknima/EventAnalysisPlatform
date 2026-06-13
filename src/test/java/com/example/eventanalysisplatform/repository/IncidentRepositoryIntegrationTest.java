package com.example.eventanalysisplatform.repository;

import com.example.eventanalysisplatform.exception.IncidentConflictException;
import com.example.eventanalysisplatform.record.IncidentEvent;
import com.example.eventanalysisplatform.record.IncidentRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class IncidentRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:15")
                    .withDatabaseName("incidents")
                    .withUsername("postgres")
                    .withPassword("postgres");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private IncidentRepository incidentRepository;

    @Test
    void saveInsertsIncident() {
        IncidentEvent event = new IncidentEvent(
                "INC-REPO-1",
                "payment",
                "HIGH",
                "timeout",
                "corr-repo-1"
        );

        incidentRepository.save(event);

        IncidentRequest saved = incidentRepository.findById("INC-REPO-1");

        assertThat(saved).isEqualTo(new IncidentRequest(
                "INC-REPO-1",
                "payment",
                "HIGH",
                "timeout"
        ));
    }

    @Test
    void saveIsIdempotentForSameIncidentData() {
        IncidentEvent event = new IncidentEvent(
                "INC-REPO-2",
                "payment",
                "HIGH",
                "timeout",
                "corr-repo-2"
        );

        incidentRepository.save(event);
        incidentRepository.save(event);

        IncidentRequest saved = incidentRepository.findById("INC-REPO-2");

        assertThat(saved).isEqualTo(new IncidentRequest(
                "INC-REPO-2",
                "payment",
                "HIGH",
                "timeout"
        ));
    }

    @Test
    void saveThrowsConflictForSameIdWithDifferentData() {
        IncidentEvent original = new IncidentEvent(
                "INC-REPO-3",
                "payment",
                "HIGH",
                "timeout",
                "corr-original"
        );

        IncidentEvent conflicting = new IncidentEvent(
                "INC-REPO-3",
                "checkout",
                "LOW",
                "different",
                "corr-conflict"
        );

        incidentRepository.save(original);

        assertThatThrownBy(() -> incidentRepository.save(conflicting))
                .isInstanceOf(IncidentConflictException.class)
                .hasMessage("Incident conflict for id: INC-REPO-3");
    }
}