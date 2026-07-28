package com.specpulse.api;

import com.specpulse.diff.DiffResultDTO;
import com.specpulse.diff.DiffService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DiffController.class)
@TestPropertySource(properties = "specpulse.auth.token=test-token")
class DiffControllerAuthTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DiffService diffService;

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

        String requestJson = "{\"oldSpec\":\"openapi: 3.0.0\\npaths: {}\",\"newSpec\":\"openapi: 3.0.0\\npaths: {}\"}";

        mockMvc.perform(post("/api/v1/diffs/compare")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diff").value("diff"));
    }
}
