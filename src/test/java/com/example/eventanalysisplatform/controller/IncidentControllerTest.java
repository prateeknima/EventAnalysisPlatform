package com.example.eventanalysisplatform.controller;

import com.example.eventanalysisplatform.exception.ApiExceptionHandler;
import com.example.eventanalysisplatform.record.IncidentRequest;
import com.example.eventanalysisplatform.repository.IncidentRepository;
import com.example.eventanalysisplatform.search.IncidentSearchDocument;
import com.example.eventanalysisplatform.service.IncidentSearchService;
import com.example.eventanalysisplatform.service.IncidentService;
import com.example.eventanalysisplatform.service.RedisService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = IncidentController.class,
        excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class
)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiExceptionHandler.class)
class IncidentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IncidentService incidentService;

    @MockitoBean
    private RedisService redisService;

    @MockitoBean
    private IncidentRepository incidentRepository;

    @MockitoBean
    private IncidentSearchService incidentSearchService;

    @Test
    void createReturnsAcceptedForValidRequest() throws Exception {
        IncidentRequest request = new IncidentRequest(
                "INC-TEST",
                "payment",
                "HIGH",
                "timeout"
        );

        mockMvc.perform(post("/incidents")
                        .header("X-Correlation-Id", "test-correlation-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());

        Mockito.verify(incidentService).handle(
                any(IncidentRequest.class),
                eq("test-correlation-id")
        );
    }

    @Test
    void createReturnsBadRequestForInvalidRequest() throws Exception {
        IncidentRequest request = new IncidentRequest(
                "",
                "",
                "INVALID",
                ""
        );

        mockMvc.perform(post("/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.incidentId").value("incidentId is required"))
                .andExpect(jsonPath("$.validationErrors.source").value("source is required"))
                .andExpect(jsonPath("$.validationErrors.severity").value("severity must be one of LOW, MEDIUM, HIGH, CRITICAL"))
                .andExpect(jsonPath("$.validationErrors.message").value("message is required"));
    }

    @Test
    void getByIdReturnsIncidentWhenFound() throws Exception {
        Mockito.when(incidentRepository.findById("INC-1"))
                .thenReturn(new IncidentRequest(
                        "INC-1",
                        "payment",
                        "HIGH",
                        "timeout"
                ));

        mockMvc.perform(get("/incidents/INC-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidentId").value("INC-1"))
                .andExpect(jsonPath("$.source").value("payment"))
                .andExpect(jsonPath("$.severity").value("HIGH"))
                .andExpect(jsonPath("$.message").value("timeout"));
    }

    @Test
    void getByIdReturnsNotFoundWhenMissing() throws Exception {
        Mockito.when(incidentRepository.findById("MISSING"))
                .thenReturn(null);

        mockMvc.perform(get("/incidents/MISSING"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Incident not found: MISSING"));
    }

    @Test
    void getStatusReturnsRedisStatus() throws Exception {
        Mockito.when(redisService.getStatus("INC-1"))
                .thenReturn("PROCESSED");

        mockMvc.perform(get("/incidents/INC-1/status"))
                .andExpect(status().isOk())
                .andExpect(content().string("PROCESSED"));
    }

    @Test
    void getStatusReturnsNotFoundWhenMissing() throws Exception {
        Mockito.when(redisService.getStatus("MISSING"))
                .thenReturn(null);

        mockMvc.perform(get("/incidents/MISSING/status"))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchReturnsIndexedIncidents() throws Exception {
        Mockito.when(incidentSearchService.search("timeout"))
                .thenReturn(List.of(
                        new IncidentSearchDocument(
                                "INC-SEARCH-1",
                                "payment",
                                "HIGH",
                                "payment timeout"
                        )
                ));

        mockMvc.perform(get("/incidents/search")
                        .param("q", "timeout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].incidentId").value("INC-SEARCH-1"))
                .andExpect(jsonPath("$[0].message").value("payment timeout"));
    }
}