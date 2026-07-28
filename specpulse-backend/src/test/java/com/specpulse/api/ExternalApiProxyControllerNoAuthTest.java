package com.specpulse.api;

import com.specpulse.proxy.ExternalApiProxyRequest;
import com.specpulse.proxy.ExternalApiProxyResponse;
import com.specpulse.proxy.ExternalApiProxyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExternalApiProxyController.class)
class ExternalApiProxyControllerNoAuthTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExternalApiProxyService proxyService;

    @Test
    @DisplayName("Should allow proxy when SPECPULSE_AUTH_TOKEN is not configured")
    void shouldAllowProxyWhenAuthTokenNotConfigured() throws Exception {
        given(proxyService.proxy(any(ExternalApiProxyRequest.class))).willReturn(
                new ExternalApiProxyResponse(200, "OK", java.util.Map.of(), null, null)
        );

        String requestJson = "{\"url\":\"https://example.com\",\"method\":\"GET\",\"headers\":{},\"body\":null}";

        mockMvc.perform(post("/api/v1/tests/proxy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }
}
