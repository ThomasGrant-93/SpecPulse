package com.specpulse.api;

import com.specpulse.diff.DiffResultDTO;
import com.specpulse.diff.DiffService;
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DiffController.class)
@Import(com.specpulse.config.SecurityConfig.class)
class DiffControllerAuthTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private DiffService diffService;

    @MockBean
    private UserRepository userRepository;

    @Test
    @DisplayName("Should return 401 when diff auth is configured but Authorization header is missing")
    void shouldReturn401WhenDiffAuthMissing() throws Exception {
        String requestJson = "{\"oldSpec\":\"openapi: 3.0.0\\npaths: {}\",\"newSpec\":\"openapi: 3.0.0\\npaths: {}\"}";

        mockMvc.perform(post("/api/v1/diffs/compare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 when diff auth is configured but Authorization token is wrong")
    void shouldReturn401WhenDiffAuthWrong() throws Exception {
        String requestJson = "{\"oldSpec\":\"openapi: 3.0.0\\npaths: {}\",\"newSpec\":\"openapi: 3.0.0\\npaths: {}\"}";

        mockMvc.perform(post("/api/v1/diffs/compare")
                        .header("Authorization", "Bearer wrong")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should allow diff comparison when Authorization header matches configured Bearer token")
    void shouldAllowDiffComparisonWhenDiffAuthMatches() throws Exception {
        given(diffService.compare(anyString(), anyString())).willReturn(
                new DiffResultDTO("diff", false, 0, true, false, false)
        );

        long userId = 1L;
        JwtTestUtil.stubEnabledAdmin(userRepository, userId);
        String token = JwtTestUtil.adminAccessToken(jwtService, userId);

        String requestJson = "{\"oldSpec\":\"openapi: 3.0.0\\npaths: {}\",\"newSpec\":\"openapi: 3.0.0\\npaths: {}\"}";

        mockMvc.perform(post("/api/v1/diffs/compare")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diff").value("diff"));
    }
}
