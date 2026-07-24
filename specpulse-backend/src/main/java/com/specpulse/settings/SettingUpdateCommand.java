package com.specpulse.settings;

public record SettingUpdateCommand(
        String category,
        String key,
        Object value
) {
    public String fullKey() {
        return category + "." + key;
    }
}
