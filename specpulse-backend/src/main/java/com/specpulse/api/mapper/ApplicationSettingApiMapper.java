package com.specpulse.api.mapper;

import com.specpulse.settings.SettingUpdateCommand;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class ApplicationSettingApiMapper {

    public BulkUpdateMappingResult toBulkUpdateCommands(Map<String, Object> updates) {
        List<SettingUpdateCommand> commands = new ArrayList<>();
        List<String> invalidKeys = new ArrayList<>();

        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            String[] parts = entry.getKey().split("\\.", 2);
            if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
                invalidKeys.add(entry.getKey());
                continue;
            }

            commands.add(new SettingUpdateCommand(parts[0], parts[1], entry.getValue()));
        }

        return new BulkUpdateMappingResult(commands, invalidKeys);
    }

    public record BulkUpdateMappingResult(
            List<SettingUpdateCommand> commands,
            List<String> invalidKeys
    ) {
    }
}
