package com.example.eventanalysisplatform.controller;

import com.example.eventanalysisplatform.record.IncidentRequest;
import com.example.eventanalysisplatform.service.IncidentService;
import com.example.eventanalysisplatform.service.RedisService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/incidents")
public class IncidentController {

    private final IncidentService service;
    private final RedisService redisService;

    public IncidentController(
            IncidentService service,
            RedisService redisService
    ) {
        this.service = service;
        this.redisService = redisService;
    }

    @PostMapping
    public String create(
            @RequestBody IncidentRequest incidentRequest
    ) {


        service.handle(incidentRequest);

        return "accepted";
    }

    @GetMapping("/{id}")
    public String getIncident(
            @PathVariable String id
    ){
        return redisService.get("incident:" + id);
    }
}