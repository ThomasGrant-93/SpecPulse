package com.specpulse.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.specpulse.api.mapper.ApplicationSettingApiMapper;
import com.specpulse.settings.ApplicationSettingDTO;
import com.specpulse.settings.ApplicationSettingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(com.specpulse.settings.ApplicationSettingController.class)
@TestPropertySource(properties = "specpulse.auth.token=test-settings-token")
class ApplicationSettingControllerAuthTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ApplicationSettingService settingService;

    @MockBean
    private ApplicationSettingApiMapper settingApiMapper;

    @Test
    @DisplayName("Should return 401 for settings mutation without Authorization")
    void shouldReturn401WhenMissingAuth() throws Exception {
        String reqJson = "{\"value\":\"light\"}";

        mockMvc.perform(put("/api/v1/settings/general/theme")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should allow settings mutation with correct Authorization")
    void shouldAllowWithCorrectAuth() throws Exception {
        ApplicationSettingDTO dto = ApplicationSettingDTO.builder()
                .category("general")
                .key("theme")
                .value("light")
                .build();

        given(settingService.updateSetting(eq("general"), eq("theme"), any()))
                .willReturn(dto);

        String reqJson = objectMapper.writeValueAsString(new java.util.HashMap<String, Object>() {{
            put("value", "light");
        }});

        mockMvc.perform(put("/api/v1/settings/general/theme")
                        .header("Authorization", "Bearer test-settings-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("general"))
                .andExpect(jsonPath("$.key").value("theme"))
                .andExpect(jsonPath("$.value").value("light"));
    }
}
