package com.specpulse.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.specpulse.api.mapper.RegistryApiMapper;
import com.specpulse.registry.RegistryService;
import com.specpulse.registry.RegistryService.CreateServiceRequest;
import com.specpulse.registry.ServiceDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RegistryController.class)
@TestPropertySource(properties = "specpulse.auth.token=test-registry-token")
class RegistryControllerAuthTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RegistryService registryService;

    @MockBean
    private RegistryApiMapper registryApiMapper;

    @Test
    @DisplayName("Should return 401 when create service is called without Authorization")
    void shouldReturn401OnCreateWithoutAuth() throws Exception {
        CreateServiceRequest req = new CreateServiceRequest(
                "svc",
                "https://api.example.com/openapi.json",
                "desc",
                true,
                null
        );

        mockMvc.perform(post("/api/v1/registry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 201 when create service is authorized")
    void shouldReturn201OnCreateWithAuth() throws Exception {
        CreateServiceRequest req = new CreateServiceRequest(
                "svc",
                "https://api.example.com/openapi.json",
                "desc",
                true,
                null
        );

        ServiceDTO created = new ServiceDTO(
                1L,
                "svc",
                "https://api.example.com/openapi.json",
                "desc",
                true,
                null,
                null
        );

        given(registryService.createService(any())).willReturn(created);

        mockMvc.perform(post("/api/v1/registry")
                        .header("Authorization", "Bearer test-registry-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("svc"));
    }
}
