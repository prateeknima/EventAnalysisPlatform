package com.example.eventanalysisplatform.service;

import com.example.eventanalysisplatform.record.IncidentEnrichmentResult;
import com.example.eventanalysisplatform.record.IncidentEvent;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IncidentEnrichmentService {

    private final ServiceDependencyGraph serviceDependencyGraph;
    private final IncidentPriorityService incidentPriorityService;

    public IncidentEnrichmentService(
            ServiceDependencyGraph serviceDependencyGraph,
            IncidentPriorityService incidentPriorityService
    ) {
        this.serviceDependencyGraph = serviceDependencyGraph;
        this.incidentPriorityService = incidentPriorityService;
    }

    public IncidentEnrichmentResult enrich(IncidentEvent incidentEvent) {
        List<String> affectedServices =
                serviceDependencyGraph.findAffectedServices(incidentEvent.source());

        int priorityScore =
                incidentPriorityService.calculatePriorityScore(
                        incidentEvent.severity(),
                        affectedServices.size()
                );

        String riskLevel =
                incidentPriorityService.determineRiskLevel(priorityScore);

        String recommendedAction =
                recommendedAction(riskLevel, affectedServices);

        return new IncidentEnrichmentResult(
                incidentEvent.incidentId(),
                priorityScore,
                riskLevel,
                affectedServices,
                recommendedAction
        );
    }

    private String recommendedAction(
            String riskLevel,
            List<String> affectedServices
    ) {
        if ("CRITICAL".equals(riskLevel)) {
            return "Page primary on-call immediately";
        }

        if ("HIGH".equals(riskLevel) && affectedServices.size() > 5) {
            return "Escalate to service owner and monitor downstream impact";
        }

        if ("HIGH".equals(riskLevel)) {
            return "Investigate affected service and recent deployments";
        }

        if ("MEDIUM".equals(riskLevel)) {
            return "Create incident ticket and monitor error trends";
        }

        return "Monitor and review during normal triage";
    }
}