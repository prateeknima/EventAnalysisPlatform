package com.example.eventanalysisplatform.repository;

import com.example.eventanalysisplatform.exception.IncidentConflictException;
import com.example.eventanalysisplatform.record.IncidentEvent;
import com.example.eventanalysisplatform.record.IncidentRequest;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
class IncidentRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:15")
                    .withDatabaseName("incidents")
                    .withUsername("postgres")
                    .withPassword("postgres");

    private HikariDataSource dataSource;
    private IncidentRepository incidentRepository;

    @BeforeEach
    void setUp() {
        dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(postgres.getJdbcUrl());
        dataSource.setUsername(postgres.getUsername());
        dataSource.setPassword(postgres.getPassword());

        Flyway.configure()
                .dataSource(dataSource)
                .cleanDisabled(false)
                .load()
                .clean();

        Flyway.configure()
                .dataSource(dataSource)
                .load()
                .migrate();

        DSLContext dsl = DSL.using(dataSource, SQLDialect.POSTGRES);
        incidentRepository = new IncidentRepository(dsl);
    }

    @AfterEach
    void tearDown() {
        dataSource.close();
    }

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