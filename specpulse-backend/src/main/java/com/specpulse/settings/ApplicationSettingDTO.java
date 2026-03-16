package com.specpulse.settings;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationSettingDTO {
    private Long id;
    private String category;
    private String key;
    private Object value;
    private String valueType;
    private String description;
    private Boolean isPublic;
    private Boolean isEditable;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * DTO для группы настроек по категориям
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SettingsCategory {
        private String name;
        private String displayName;
        private String description;
        private List<ApplicationSettingDTO> settings;
    }

    /**
     * DTO для обновления настройки
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateSettingRequest {
        private Object value;
    }

    /**
     * DTO для массового обновления
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateSettingsRequest {
        private Map<String, Object> settings;
    }
}
