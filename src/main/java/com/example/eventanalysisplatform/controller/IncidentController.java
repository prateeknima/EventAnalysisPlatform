package com.example.eventanalysisplatform.controller;

import com.example.eventanalysisplatform.exception.IncidentNotFoundException;
import com.example.eventanalysisplatform.record.IncidentRequest;
import com.example.eventanalysisplatform.repository.IncidentRepository;
import com.example.eventanalysisplatform.search.IncidentRankedSearchResult;
import com.example.eventanalysisplatform.search.IncidentSearchDocument;
import com.example.eventanalysisplatform.service.IncidentSearchService;
import com.example.eventanalysisplatform.service.IncidentService;
import com.example.eventanalysisplatform.service.RedisService;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/incidents")
public class IncidentController {

    private final IncidentService service;
    private final RedisService redisService;
    private final IncidentRepository incidentRepository;
    private final IncidentSearchService incidentSearchService;

    public IncidentController(
            IncidentService service,
            RedisService redisService,
            IncidentRepository incidentRepository,
            IncidentSearchService incidentSearchService
    ) {
        this.service = service;
        this.redisService = redisService;
        this.incidentRepository =incidentRepository;
        this.incidentSearchService = incidentSearchService;
    }

    @PostMapping
    public ResponseEntity<Void> create(
            @RequestHeader(value = "X-Correlation-Id", required = false)
            String correlationId,
            @Valid @RequestBody IncidentRequest incidentRequest
    ) {


        service.handle(incidentRequest, correlationId);

        return ResponseEntity.accepted().build();
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<String> getIncident(
            @PathVariable String id
    ){
        String status = redisService.getStatus(id);

        if (status == null){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(status);
    }

    @GetMapping("/{incidentId}")
    public IncidentRequest getById(
            @PathVariable String incidentId
    ){
        IncidentRequest incidentRequest = incidentRepository.findById(incidentId);

        if (incidentRequest == null){
            throw new IncidentNotFoundException(incidentId);
        }

        return incidentRequest;
    }

    @GetMapping("/search")
    public List<IncidentSearchDocument> search(
            @RequestParam String q
    ){
        return incidentSearchService.search(q);
    }

    @GetMapping("/search/ranked")
    public List<IncidentRankedSearchResult> rankedSearch(
            @RequestParam String q,
            @RequestParam(defaultValue = "100") int limit
    ){
        return incidentSearchService.searchRanked(q, limit);
    }
}
