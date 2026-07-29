package com.specpulse.api;

import com.specpulse.api.mapper.ApplicationSettingApiMapper;
import com.specpulse.auth.repository.UserRepository;
import com.specpulse.auth.security.JwtService;
import com.specpulse.test.JwtTestUtil;
import com.specpulse.settings.ApplicationSettingService;
import com.specpulse.settings.SettingUpdateCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(com.specpulse.settings.ApplicationSettingController.class)
@Import({ApplicationSettingApiMapper.class, com.specpulse.config.SecurityConfig.class})
@SuppressWarnings("removal")
class ApplicationSettingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private ApplicationSettingService settingService;

    @MockBean
    private UserRepository userRepository;

    private String adminToken;

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("Should map valid keys and skip invalid in bulk update")
    void shouldMapValidKeysAndSkipInvalidInBulkUpdate() throws Exception {
        long userId = 1L;
        JwtTestUtil.stubEnabledAdmin(userRepository, userId);
        adminToken = JwtTestUtil.adminAccessToken(jwtService, userId);

        given(settingService.updateSettings(any())).willReturn(List.of());

        String requestBody = """
                {
                  "general.theme": "light",
                  "invalid": "value",
                  ".empty": "value",
                  "pull.": "value",
                  "scheduler.enabled": true
                }
                """;

        mockMvc.perform(patch("/api/v1/settings")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        ArgumentCaptor<List<SettingUpdateCommand>> updatesCaptor = ArgumentCaptor.forClass(List.class);
        verify(settingService).updateSettings(updatesCaptor.capture());

        List<SettingUpdateCommand> commands = updatesCaptor.getValue();
        assertThat(commands)
                .extracting(SettingUpdateCommand::fullKey)
                .containsExactly("general.theme", "scheduler.enabled");
        assertThat(commands)
                .extracting(SettingUpdateCommand::value)
                .containsExactly("light", true);
    }

    @Test
    @DisplayName("Should update single setting")
    void shouldUpdateSingleSetting() throws Exception {
        long userId = 1L;
        JwtTestUtil.stubEnabledAdmin(userRepository, userId);
        adminToken = JwtTestUtil.adminAccessToken(jwtService, userId);

        given(settingService.updateSetting("general", "theme", "light")).willReturn(null);

        mockMvc.perform(put("/api/v1/settings/general/theme")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":\"light\"}"))
                .andExpect(status().isOk());

        verify(settingService).updateSetting("general", "theme", "light");
    }
}
