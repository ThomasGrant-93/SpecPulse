package com.specpulse.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.specpulse.api.mapper.RegistryApiMapper;
import com.specpulse.auth.api.AuthController;
import com.specpulse.auth.application.AuthService;
import com.specpulse.auth.domain.RoleEntity;
import com.specpulse.auth.domain.UserEntity;
import com.specpulse.auth.domain.PermissionEntity;
import com.specpulse.auth.infrastructure.UserRepository;
import com.specpulse.auth.security.JwtAuthenticationService;
import com.specpulse.auth.security.JwtService;
import com.specpulse.auth.security.RbacPermissions;
import com.specpulse.auth.security.RbacRoles;
import com.specpulse.registry.application.RegistryService;
import com.specpulse.registry.ServiceValidationRequest;
import com.specpulse.registry.ServiceValidationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {RegistryController.class, AuthController.class})
@Import(com.specpulse.config.SecurityConfig.class)
class RegistryControllerValidateAuthTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtAuthenticationService jwtAuthenticationService;

    @MockBean
    private RegistryService registryService;

    @MockBean
    private RegistryApiMapper registryApiMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private UserRepository userRepository;

    private void stubEnabledAdmin(long userId) {
        RoleEntity adminRole = new RoleEntity();
        adminRole.setName(RbacRoles.ADMIN);
        adminRole.setEnabled(true);
        adminRole.setPermissions(Set.of(
                permission(RbacPermissions.API_DOCS_ACCESS),
                permission(RbacPermissions.API_TEST_EXECUTE)
        ));

        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setUsername("admin");
        user.setEmail("admin@example.com");
        user.setEnabled(true);
        user.setRoles(Set.of(adminRole));

        given(userRepository.findByIdAndDeletedAtIsNull(userId))
                .willReturn(Optional.of(user));
    }

    private PermissionEntity permission(String permissionName) {
        PermissionEntity p = new PermissionEntity();
        p.setName(permissionName);
        p.setEnabled(true);
        p.setDeletedAt(null);
        return p;
    }

    private String adminAccessToken(long userId) {
        return jwtService.issueAccessToken(
                        userId,
                        "admin",
                        "admin@example.com",
                        true,
                        List.of(RbacRoles.ADMIN),
                        Set.of(),
                        Map.of()
                )
                .accessToken();
    }

    private String adminRefreshToken(long userId) {
        return jwtService.issueRefreshToken(
                        userId,
                        "admin",
                        "admin@example.com",
                        true,
                        List.of(RbacRoles.ADMIN),
                        Set.of(),
                        Map.of()
                )
                .refreshToken();
    }

    @Test
    @DisplayName("Auth pipeline: refresh token sent as Bearer must not authenticate; registry/validate returns 401")
    void shouldReturn401WhenRefreshTokenSentAsBearer() throws Exception {
        long userId = 1L;
        stubEnabledAdmin(userId);

        String refreshToken = adminRefreshToken(userId);
        String bearer = "Bearer " + refreshToken;

        // ---- Diagnostics: JWT parsing results ----
        assertJwtAccessParseEmpty(refreshToken);

        var parsedRefresh = jwtService.parseRefreshToken(refreshToken)
                .orElseThrow(() -> new AssertionError("Expected refresh token to parse"));
        org.junit.jupiter.api.Assertions.assertEquals("admin", parsedRefresh.getUsername());
        org.junit.jupiter.api.Assertions.assertTrue(parsedRefresh.getRoles().contains(RbacRoles.ADMIN));
        org.junit.jupiter.api.Assertions.assertTrue(
                parsedRefresh.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_" + RbacRoles.ADMIN))
        );

        // ---- Diagnostics: authentication service result ----
        var authOpt = jwtAuthenticationService.authenticateAccessToken(refreshToken);
        org.junit.jupiter.api.Assertions.assertTrue(authOpt.isEmpty(), "Expected authenticateAccessToken() to return empty");

        // ---- Actual failing request ----
        var payload = new RegistryController.ValidateRequest(
                "svc",
                "https://api.example.com/openapi.json"
        );

        mockMvc.perform(post("/api/v1/registry/validate")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("unauthorized"))
                .andExpect(jsonPath("$.path").value("/api/v1/registry/validate"));

        // Authentication context was never populated.
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/auth/me")
                        .header("Authorization", bearer))
                .andExpect(status().isUnauthorized());

        // Authentication must fail before user lookup.
        verify(userRepository, never()).findByIdAndDeletedAtIsNull(any());
    }

    @Test
    @DisplayName("Auth pipeline: access token must authenticate and /auth/me must return JwtPrincipal")
    void shouldAuthenticateWithAccessToken() throws Exception {
        long userId = 1L;
        stubEnabledAdmin(userId);
        String accessToken = adminAccessToken(userId);

        var parsedAccess = jwtService.parseAccessToken(accessToken)
                .orElseThrow(() -> new AssertionError("Expected access token to parse"));

        org.junit.jupiter.api.Assertions.assertTrue(parsedAccess.getRoles().contains(RbacRoles.ADMIN));
        org.junit.jupiter.api.Assertions.assertTrue(
                parsedAccess.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_" + RbacRoles.ADMIN))
        );

        // /auth/me is GET
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles").value(org.hamcrest.Matchers.hasItem(RbacRoles.ADMIN)));

        // Positive control for protected endpoint.
        var request = new RegistryController.ValidateRequest(
                "svc",
                "https://api.example.com/openapi.json"
        );
        ServiceValidationRequest mapped = new ServiceValidationRequest(request.name(), request.openApiUrl());
        ServiceValidationResult result = new ServiceValidationResult(true, List.of());
        RegistryController.ValidateResponse mappedResponse = new RegistryController.ValidateResponse(true, List.of());

        given(registryApiMapper.toServiceValidationRequest(any()))
                .willReturn(mapped);
        given(registryService.validateService(eq(mapped)))
                .willReturn(result);
        given(registryApiMapper.toValidateResponse(eq(result)))
                .willReturn(mappedResponse);

        mockMvc.perform(post("/api/v1/registry/validate")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    private void assertJwtAccessParseEmpty(String token) {
        var parsedAccess = jwtService.parseAccessToken(token);
        org.junit.jupiter.api.Assertions.assertTrue(parsedAccess.isEmpty(),
                "Expected parseAccessToken() to be empty for a non-access token");
    }
}
