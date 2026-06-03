package com.example.eventanalysisplatform.controller;

import com.example.eventanalysisplatform.exception.IncidentNotFoundException;
import com.example.eventanalysisplatform.record.IncidentRequest;
import com.example.eventanalysisplatform.repository.IncidentRepository;
import com.example.eventanalysisplatform.service.IncidentService;
import com.example.eventanalysisplatform.service.RedisService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/incidents")
public class IncidentController {

    private final IncidentService service;
    private final RedisService redisService;
    private final IncidentRepository incidentRepository;

    public IncidentController(
            IncidentService service,
            RedisService redisService,
            IncidentRepository incidentRepository
    ) {
        this.service = service;
        this.redisService = redisService;
        this.incidentRepository =incidentRepository;
    }

    @PostMapping
    public ResponseEntity<Void> create(
            @Valid @RequestBody IncidentRequest incidentRequest
    ) {


        service.handle(incidentRequest);

        return ResponseEntity.accepted().build();
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<String> getIncident(
            @PathVariable String id
    ){
        String status = redisService.get("incident:" + id);

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
}