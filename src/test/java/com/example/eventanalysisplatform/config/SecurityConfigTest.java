package com.example.eventanalysisplatform.config;

import com.example.eventanalysisplatform.controller.IncidentController;
import com.example.eventanalysisplatform.exception.ApiExceptionHandler;
import com.example.eventanalysisplatform.record.IncidentRequest;
import com.example.eventanalysisplatform.repository.IncidentRepository;
import com.example.eventanalysisplatform.service.IncidentSearchService;
import com.example.eventanalysisplatform.service.IncidentService;
import com.example.eventanalysisplatform.service.RedisService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IncidentController.class)
@Import({
        SecurityConfig.class,
        ApiExceptionHandler.class
})
class SecurityConfigTest {

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
    void getIncidentWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/incidents/INC-1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getIncidentWithoutReadRoleReturnsForbidden() throws Exception {
        mockMvc.perform(get("/incidents/INC-1")
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("incidents.write")
                        )))
                .andExpect(status().isForbidden());
    }

    @Test
    void getIncidentWithReadRoleIsAllowed() throws Exception {
        mockMvc.perform(get("/incidents/INC-1")
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("incidents.read")
                        )))
                .andExpect(status().isNotFound());
    }

    @Test
    void createIncidentWithoutWriteRoleReturnsForbidden() throws Exception {
        IncidentRequest request = new IncidentRequest(
                "INC-SECURITY-1",
                "payment",
                "HIGH",
                "security test"
        );

        mockMvc.perform(post("/incidents")
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("incidents.read")
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createIncidentWithWriteRoleIsAllowed() throws Exception {
        IncidentRequest request = new IncidentRequest(
                "INC-SECURITY-2",
                "payment",
                "HIGH",
                "security test"
        );

        mockMvc.perform(post("/incidents")
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("incidents.write")
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());

        verify(incidentService).handle(request, null);
    }
}