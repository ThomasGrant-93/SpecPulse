package com.specpulse.group;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceGroupDTO {
    private Long id;
    private String name;
    private String description;
    private Long parentGroupId;
    private String parentGroupName;
    private String color;
    private String icon;
    private Integer sortOrder;
    private Instant createdAt;
    private Instant updatedAt;

    // Информация о вложенных группах
    private List<ServiceGroupDTO> childGroups;

    // Количество сервисов в группе
    private Integer serviceCount;

    // Сервисы в группе (опционально, для детального просмотра)
    private List<GroupServiceDTO> services;

    /**
     * DTO сервиса внутри группы
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupServiceDTO {
        private Long id;
        private String name;
        private String openApiUrl;
        private String description;
        private Boolean enabled;
        private Instant addedAt;
    }
}
