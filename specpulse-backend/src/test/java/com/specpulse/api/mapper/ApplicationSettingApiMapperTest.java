package com.specpulse.api.mapper;

import com.specpulse.settings.SettingUpdateCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationSettingApiMapperTest {

    private final ApplicationSettingApiMapper mapper = new ApplicationSettingApiMapper();

    @Test
    @DisplayName("Should map valid bulk update keys to commands")
    void shouldMapValidKeysToCommands() {
        LinkedHashMap<String, Object> updates = new LinkedHashMap<>();
        updates.put("general.theme", "light");
        updates.put("scheduler.enabled", true);

        var result = mapper.toBulkUpdateCommands(updates);

        assertThat(result.invalidKeys()).isEmpty();
        assertThat(result.commands())
                .extracting(SettingUpdateCommand::fullKey)
                .containsExactly("general.theme", "scheduler.enabled");
        assertThat(result.commands())
                .extracting(SettingUpdateCommand::value)
                .containsExactly("light", true);
    }

    @Test
    @DisplayName("Should collect invalid keys and keep valid commands")
    void shouldCollectInvalidKeys() {
        LinkedHashMap<String, Object> updates = new LinkedHashMap<>();
        updates.put("general.theme", "light");
        updates.put("invalid", "x");
        updates.put(".empty", "x");
        updates.put("pull.", "x");
        updates.put("scheduler.enabled", false);

        var result = mapper.toBulkUpdateCommands(updates);

        assertThat(result.commands())
                .extracting(SettingUpdateCommand::fullKey)
                .containsExactly("general.theme", "scheduler.enabled");
        assertThat(result.invalidKeys()).containsExactly("invalid", ".empty", "pull.");
    }
}
