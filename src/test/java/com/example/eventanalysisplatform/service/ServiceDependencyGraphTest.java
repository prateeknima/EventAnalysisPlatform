package com.example.eventanalysisplatform.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceDependencyGraphTest {

    private final ServiceDependencyGraph graph = new ServiceDependencyGraph();

    @Test
    void findAffectedServicesReturnsDownstreamServicesWithoutDuplicates() {
        List<String> affectedServices = graph.findAffectedServices("payment");

        assertThat(affectedServices)
                .containsExactly(
                        "payment",
                        "fraud",
                        "checkout",
                        "notification",
                        "risk-engine",
                        "inventory",
                        "shipping",
                        "email",
                        "sms",
                        "warehouse",
                        "carrier"
                );

        assertThat(affectedServices)
                .doesNotHaveDuplicates();
    }

    @Test
    void findAffectedServicesReturnsUnknownSourceAsSingleAffectedService() {
        List<String> affectedServices = graph.findAffectedServices("unknown-service");

        assertThat(affectedServices)
                .containsExactly("unknown-service");
    }

    @Test
    void findAffectedServicesReturnsEmptyListForBlankSource() {
        assertThat(graph.findAffectedServices(""))
                .isEmpty();

        assertThat(graph.findAffectedServices("   "))
                .isEmpty();

        assertThat(graph.findAffectedServices(null))
                .isEmpty();
    }

    @Test
    void findAffectedServicesNormalizesSourceToLowerCase() {
        List<String> affectedServices = graph.findAffectedServices("PAYMENT");

        assertThat(affectedServices)
                .contains("payment", "fraud", "checkout", "notification");
    }
}