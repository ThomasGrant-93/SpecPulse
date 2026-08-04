package com.specpulse.api;

import com.specpulse.auth.infrastructure.UserRepository;
import com.specpulse.proxy.ExternalApiProxyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExternalApiProxyController.class)
@Import(com.specpulse.config.SecurityConfig.class)
class ExternalApiProxyControllerNoAuthTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExternalApiProxyService proxyService;

    @MockBean
    private UserRepository userRepository;

    @Test
    @DisplayName("Should return 401 when proxy auth is missing")
    void shouldReturn401WhenAuthMissing() throws Exception {

        String requestJson = "{\"url\":\"https://example.com\",\"method\":\"GET\",\"headers\":{},\"body\":null}";

        mockMvc.perform(post("/api/v1/tests/proxy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isUnauthorized());
    }
}
