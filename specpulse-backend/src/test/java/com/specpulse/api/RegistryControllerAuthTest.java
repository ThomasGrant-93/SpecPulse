package com.specpulse.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.specpulse.api.mapper.RegistryApiMapper;
import com.specpulse.registry.RegistryService;
import com.specpulse.auth.entity.UserEntity;
import com.specpulse.auth.entity.RoleEntity;
import com.specpulse.auth.repository.UserRepository;
import com.specpulse.registry.RegistryService.CreateServiceRequest;
import com.specpulse.registry.ServiceDTO;
import com.specpulse.auth.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RegistryController.class)
@Import(com.specpulse.config.SecurityConfig.class)
class RegistryControllerAuthTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private RegistryService registryService;

    @MockBean
    private RegistryApiMapper registryApiMapper;

    @MockBean
    private UserRepository userRepository;

    private String adminAccessToken(long userId) {
        return jwtService.issueAccessToken(
                        userId,
                        "admin",
                        "admin@example.com",
                        true,
                        java.util.List.of("ADMIN"),
                        java.util.Set.of(),
                        java.util.Map.of()
                )
                .accessToken();
    }

    private void stubEnabledAdmin(long userId) {
        RoleEntity adminRole = new RoleEntity();
        adminRole.setName("ADMIN");
        adminRole.setEnabled(true);

        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setUsername("admin");
        user.setEmail("admin@example.com");
        user.setEnabled(true);
        user.setRoles(java.util.Set.of(adminRole));

        given(userRepository.findByIdAndDeletedAtIsNull(userId))
                .willReturn(java.util.Optional.of(user));
    }

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
        long userId = 1L;
        stubEnabledAdmin(userId);
        String token = adminAccessToken(userId);

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
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("svc"));
    }
}
