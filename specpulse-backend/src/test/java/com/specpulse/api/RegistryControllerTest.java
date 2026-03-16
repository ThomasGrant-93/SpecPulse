package com.specpulse.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.specpulse.registry.RegistryService;
import com.specpulse.registry.ServiceDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RegistryController.class)
@SuppressWarnings("removal")
class RegistryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RegistryService registryService;

    private ServiceDTO testService;

    @BeforeEach
    void setUp() {
        testService = new ServiceDTO(
                1L,
                "test-service",
                "https://api.test.com/openapi.json",
                "Test Service",
                true,
                null,
                null
        );
    }

    @Test
    @DisplayName("Should get all services")
    void shouldGetAllServices() throws Exception {
        // Given
        var serviceWithVersion = new com.specpulse.registry.ServiceWithVersionDTO(
                testService.id(),
                testService.name(),
                testService.openApiUrl(),
                testService.description(),
                testService.enabled(),
                null,
                null,
                null,
                null,
                null
        );
        given(registryService.getAllServicesWithVersions()).willReturn(List.of(serviceWithVersion));

        // When & Then
        mockMvc.perform(get("/api/v1/registry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("test-service"));
    }

    @Test
    @DisplayName("Should get service by id")
    void shouldGetServiceById() throws Exception {
        // Given
        given(registryService.getServiceById(1L)).willReturn(testService);

        // When & Then
        mockMvc.perform(get("/api/v1/registry/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("test-service"));
    }

    @Test
    @DisplayName("Should return 404 when service not found")
    void shouldReturn404WhenServiceNotFound() throws Exception {
        // Given
        given(registryService.getServiceById(999L))
                .willThrow(new com.specpulse.exception.ResourceNotFoundException("Not found"));

        // When & Then
        mockMvc.perform(get("/api/v1/registry/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should create service")
    void shouldCreateService() throws Exception {
        // Given
        var request = new RegistryService.CreateServiceRequest(
                "new-service",
                "https://api.new.com/openapi.json",
                "New Service",
                true,
                null
        );

        var createdService = new ServiceDTO(
                2L,
                request.name(),
                request.openApiUrl(),
                request.description(),
                request.enabled(),
                null,
                null
        );

        given(registryService.createService(any())).willReturn(createdService);

        // When & Then
        mockMvc.perform(post("/api/v1/registry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("new-service"));
    }

    @Test
    @DisplayName("Should return 400 when validation fails")
    void shouldReturn400WhenValidationFails() throws Exception {
        // Given
        var invalidRequest = new RegistryService.CreateServiceRequest(
                "",  // Invalid - blank name
                "invalid-url",  // Invalid - not a URL
                "Test",
                true,
                null
        );

        // When & Then
        mockMvc.perform(post("/api/v1/registry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 409 when duplicate name")
    void shouldReturn409WhenDuplicateName() throws Exception {
        // Given
        var request = new RegistryService.CreateServiceRequest(
                "existing-service",
                "https://api.test.com/openapi.json",
                "Test",
                true,
                null
        );

        given(registryService.createService(any()))
                .willThrow(new com.specpulse.exception.DuplicateResourceException("Name exists"));

        // When & Then
        mockMvc.perform(post("/api/v1/registry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Should update service")
    void shouldUpdateService() throws Exception {
        // Given
        var request = new RegistryService.UpdateServiceRequest(
                "updated-service",
                "https://api.updated.com/openapi.json",
                "Updated Service",
                false,
                null
        );

        var updatedService = new ServiceDTO(
                1L,
                request.name(),
                request.openApiUrl(),
                request.description(),
                request.enabled(),
                null,
                null
        );

        given(registryService.updateService(anyLong(), any())).willReturn(updatedService);

        // When & Then
        mockMvc.perform(put("/api/v1/registry/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("updated-service"));
    }

    @Test
    @DisplayName("Should delete service")
    void shouldDeleteService() throws Exception {
        // Given
        doNothing().when(registryService).deleteService(1L);

        // When & Then
        mockMvc.perform(delete("/api/v1/registry/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should search services")
    void shouldSearchServices() throws Exception {
        // Given
        given(registryService.searchServices("test")).willReturn(List.of(testService));

        // When & Then
        mockMvc.perform(get("/api/v1/registry/search?q=test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("Should get search suggestions")
    void shouldGetSearchSuggestions() throws Exception {
        // Given
        given(registryService.getSearchSuggestions("test")).willReturn(List.of("test-service", "test-api"));

        // When & Then
        mockMvc.perform(get("/api/v1/registry/search/suggestions?q=test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0]").value("test-service"));
    }

    @Test
    @DisplayName("Should validate service URL")
    void shouldValidateServiceUrl() throws Exception {
        // Given
        var request = new RegistryController.ValidateRequest(
                "test-service",
                "https://api.test.com/openapi.json"
        );

        var response = new RegistryController.ValidateResponse(true, List.of());

        given(registryService.validateService(any())).willReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/registry/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }
}
