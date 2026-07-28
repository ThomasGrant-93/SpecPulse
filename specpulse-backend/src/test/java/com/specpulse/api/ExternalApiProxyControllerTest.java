package com.specpulse.api;

import com.specpulse.proxy.ExternalApiProxyResponse;
import com.specpulse.proxy.ExternalApiProxyService;
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

@WebMvcTest(ExternalApiProxyController.class)
@TestPropertySource(properties = "specpulse.auth.token=test-token")
class ExternalApiProxyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExternalApiProxyService proxyService;

    @Test
    @DisplayName("Should return 502 when proxyService reports status=0")
    void shouldReturn502OnProxyFailure() throws Exception {
        given(proxyService.proxy(any())).willReturn(
                new ExternalApiProxyResponse(0, "ERROR", java.util.Map.of(), null, "boom")
        );

        String requestJson = "{\"url\":\"https://example.com\",\"method\":\"GET\",\"headers\":{},\"body\":null}";

        mockMvc.perform(post("/api/v1/tests/proxy")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(0))
                .andExpect(jsonPath("$.error").value("boom"));
    }

    @Test
    @DisplayName("Should return 200 when proxyService returns a real upstream status")
    void shouldReturn200OnProxySuccessResponse() throws Exception {
        given(proxyService.proxy(any())).willReturn(
                new ExternalApiProxyResponse(404, "Not Found", java.util.Map.of(), null, null)
        );

        String requestJson = "{\"url\":\"https://example.com\",\"method\":\"GET\",\"headers\":{},\"body\":null}";

        mockMvc.perform(post("/api/v1/tests/proxy")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("Should return 401 when proxy auth token is configured but Authorization header is missing")
    void shouldReturn401WhenProxyAuthMissing() throws Exception {
        // This test relies on controller property override via @TestPropertySource.
        // We'll keep the service call from happening by expecting 401.
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
        given(proxyService.proxy(any())).willReturn(
                new ExternalApiProxyResponse(200, "OK", java.util.Map.of(), null, null)
        );

        String requestJson = "{\"url\":\"https://example.com\",\"method\":\"GET\",\"headers\":{},\"body\":null}";

        mockMvc.perform(post("/api/v1/tests/proxy")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }
}
