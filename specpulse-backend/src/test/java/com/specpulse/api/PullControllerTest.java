package com.specpulse.api;

import com.specpulse.scheduler.PullAllResult;
import com.specpulse.scheduler.PullResult;
import com.specpulse.scheduler.SpecPullScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PullController.class)
@TestPropertySource(properties = "specpulse.auth.token=test-pull-token")
class PullControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SpecPullScheduler pullScheduler;

    @Test
    @DisplayName("Should return 401 when pull auth token is configured but Authorization header is missing")
    void shouldReturn401WhenMissingAuthHeader() throws Exception {
        mockMvc.perform(post("/api/v1/pull/all")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 when pull auth token is wrong")
    void shouldReturn401WhenWrongAuthHeader() throws Exception {
        mockMvc.perform(post("/api/v1/pull/all")
                        .header("Authorization", "Bearer wrong")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should authorize and execute pull for a service")
    void shouldAuthorizeAndPullService() throws Exception {
        given(pullScheduler.pullServiceById(1L))
                .willReturn(PullResult.success(true, 10L, 9L, "hash-10"));

        mockMvc.perform(post("/api/v1/pull/service/1")
                        .header("Authorization", "Bearer test-pull-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Should authorize and execute pull for all")
    void shouldAuthorizeAndPullAll() throws Exception {
        given(pullScheduler.pullAllEnabledServicesManual())
                .willReturn(new PullAllResult(1, 2, 3));

        mockMvc.perform(post("/api/v1/pull/all")
                        .header("Authorization", "Bearer test-pull-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newVersions").value(1))
                .andExpect(jsonPath("$.unchanged").value(2))
                .andExpect(jsonPath("$.failed").value(3));
    }
}
