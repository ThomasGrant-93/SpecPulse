package com.specpulse.api;

import com.specpulse.version.SpecVersionDTO;
import com.specpulse.version.VersionService;
import com.specpulse.auth.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;

import java.time.Instant;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VersionController.class)
@SuppressWarnings("removal")
@Import(com.specpulse.config.SecurityConfig.class)
class VersionControllerDiffOnlyTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VersionService versionService;

    @MockBean
    private UserRepository userRepository;

    @Test
    @DisplayName("Should return 400 when diff_only is set but compare_to is missing")
    void shouldReturn400WhenCompareToMissing() throws Exception {
        mockMvc.perform(get("/api/v1/versions/1")
                        .param("diff_only", "true")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when exclude_unchanged is set but compare_to is missing")
    void shouldReturn400WhenExcludeUnchangedCompareToMissing() throws Exception {
        mockMvc.perform(get("/api/v1/versions/1")
                        .param("exclude_unchanged", "true")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 200 and use getVersionByIdDiffOnly when filtering is requested")
    void shouldReturnFilteredVersionWhenCompareToPresent() throws Exception {
        // Given
        SpecVersionDTO dto = new SpecVersionDTO(
                1L,
                10L,
                "hash",
                "3.0.0",
                "title",
                0L,
                Instant.parse("2020-01-01T00:00:00Z"),
                "{\"b\":null}"
        );

        given(versionService.getVersionByIdDiffOnly(eq(1L), eq(2L))).willReturn(dto);

        // When & Then
        mockMvc.perform(get("/api/v1/versions/1")
                .param("diff_only", "true")
                .param("compare_to", "2")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.specContent", equalTo(dto.specContent())));

        verify(versionService).getVersionByIdDiffOnly(1L, 2L);
    }

    @Test
    @DisplayName("Should return full version when diff_only and exclude_unchanged are not set")
    void shouldReturnFullVersionWhenFilteringNotRequested() throws Exception {
        // Given
        SpecVersionDTO dto = new SpecVersionDTO(
                1L,
                10L,
                "hash",
                "3.0.0",
                "title",
                0L,
                Instant.parse("2020-01-01T00:00:00Z"),
                "{\"a\":1}"
        );

        given(versionService.getVersionById(1L)).willReturn(dto);

        // When & Then
        mockMvc.perform(get("/api/v1/versions/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.specContent", equalTo(dto.specContent())));

        verify(versionService).getVersionById(1L);
    }

    @Test
    @DisplayName("Should route exclude_unchanged to getVersionByIdDiffOnly when compare_to is present")
    void shouldReturnFilteredVersionForExcludeUnchanged() throws Exception {
        // Given
        SpecVersionDTO dto = new SpecVersionDTO(
                1L,
                10L,
                "hash",
                "3.0.0",
                "title",
                0L,
                Instant.parse("2020-01-01T00:00:00Z"),
                "{\"b\":null}"
        );

        given(versionService.getVersionByIdDiffOnly(eq(1L), eq(2L))).willReturn(dto);

        // When & Then
        mockMvc.perform(get("/api/v1/versions/1")
                        .param("exclude_unchanged", "true")
                        .param("compare_to", "2")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.specContent", equalTo(dto.specContent())));

        verify(versionService).getVersionByIdDiffOnly(1L, 2L);
    }
}
