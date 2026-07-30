package com.specpulse.api;

import com.specpulse.proxy.ExternalApiProxyResponse;
import com.specpulse.proxy.ExternalApiProxyService;
import com.specpulse.auth.repository.UserRepository;
import com.specpulse.auth.security.JwtService;
import com.specpulse.test.JwtTestUtil;
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

@WebMvcTest(ExternalApiProxyController.class)
@Import(com.specpulse.config.SecurityConfig.class)
class ExternalApiProxyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private ExternalApiProxyService proxyService;

    @MockBean
    private UserRepository userRepository;

    private void stubAdminAndGetAuth(long userId) {
        JwtTestUtil.stubEnabledAdmin(userRepository, userId);
    }

    private void stubUserAndGetAuth(long userId) {
        JwtTestUtil.stubEnabledUser(userRepository, userId);
    }

    private void stubViewerAndGetAuth(long userId) {
        JwtTestUtil.stubEnabledViewer(userRepository, userId);
    }

    @Test
    @DisplayName("Should return 502 when proxyService reports status=0")
    void shouldReturn502OnProxyFailure() throws Exception {
        long userId = 1L;
        stubAdminAndGetAuth(userId);
        String token = JwtTestUtil.adminAccessToken(jwtService, userId);

        given(proxyService.proxy(any())).willReturn(
                new ExternalApiProxyResponse(0, "ERROR", java.util.Map.of(), null, "boom")
        );

        String requestJson = "{\"url\":\"https://example.com\",\"method\":\"GET\",\"headers\":{},\"body\":null}";

        mockMvc.perform(post("/api/v1/tests/proxy")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(0))
                .andExpect(jsonPath("$.error").value("boom"));
    }

    @Test
    @DisplayName("Should return 200 when proxyService returns a real upstream status")
    void shouldReturn200OnProxySuccessResponse() throws Exception {
        long userId = 1L;
        stubAdminAndGetAuth(userId);
        String token = JwtTestUtil.adminAccessToken(jwtService, userId);

        given(proxyService.proxy(any())).willReturn(
                new ExternalApiProxyResponse(404, "Not Found", java.util.Map.of(), null, null)
        );

        String requestJson = "{\"url\":\"https://example.com\",\"method\":\"GET\",\"headers\":{},\"body\":null}";

        mockMvc.perform(post("/api/v1/tests/proxy")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("Should return 401 when proxy auth token is configured but Authorization header is missing")
    void shouldReturn401WhenProxyAuthMissing() throws Exception {
        String requestJson = "{\"url\":\"https://example.com\",\"method\":\"GET\",\"headers\":{},\"body\":null}";

        mockMvc.perform(post("/api/v1/tests/proxy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 when proxy auth token is configured but Authorization token is wrong")
    void shouldReturn401WhenProxyAuthWrong() throws Exception {
        String requestJson = "{\"url\":\"https://example.com\",\"method\":\"GET\",\"headers\":{},\"body\":null}";

        mockMvc.perform(post("/api/v1/tests/proxy")
                        .header("Authorization", "Bearer wrong")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should allow proxy when Authorization header matches configured Bearer token")
    void shouldAllowProxyWhenProxyAuthMatches() throws Exception {
        long userId = 1L;
        stubAdminAndGetAuth(userId);
        String token = JwtTestUtil.adminAccessToken(jwtService, userId);

        given(proxyService.proxy(any())).willReturn(
                new ExternalApiProxyResponse(200, "OK", java.util.Map.of(), null, null)
        );

        String requestJson = "{\"url\":\"https://example.com\",\"method\":\"GET\",\"headers\":{},\"body\":null}";

        mockMvc.perform(post("/api/v1/tests/proxy")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("Should return 200 for USER role when permissions allow execution")
    void shouldAllowProxyForUserRole() throws Exception {
        long userId = 2L;
        stubUserAndGetAuth(userId);
        String token = JwtTestUtil.userAccessToken(jwtService, userId);

        given(proxyService.proxy(any())).willReturn(
                new ExternalApiProxyResponse(200, "OK", java.util.Map.of(), null, null)
        );

        String requestJson = "{\"url\":\"https://example.com\",\"method\":\"GET\",\"headers\":{},\"body\":null}";

        mockMvc.perform(post("/api/v1/tests/proxy")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    @Test
    @DisplayName("Should return 403 for VIEWER role when execution permissions are missing")
    void shouldReturn403ForViewerRole() throws Exception {
        long userId = 3L;
        stubViewerAndGetAuth(userId);
        String token = JwtTestUtil.viewerAccessToken(jwtService, userId);

        String requestJson = "{\"url\":\"https://example.com\",\"method\":\"GET\",\"headers\":{},\"body\":null}";

        mockMvc.perform(post("/api/v1/tests/proxy")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isForbidden());
    }
}
