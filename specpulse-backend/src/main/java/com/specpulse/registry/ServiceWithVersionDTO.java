package com.specpulse.registry;

import com.specpulse.entity.ServiceGroup;

import java.time.Instant;

/**
 * Service DTO with latest version information
 */
public record ServiceWithVersionDTO(
        Long id,
        String name,
        String openApiUrl,
        String description,
        boolean enabled,
        Long groupId,
        GroupInfo group,
        Instant createdAt,
        Instant updatedAt,
        LatestVersionInfo latestVersion
) {
    public static ServiceWithVersionDTO fromEntity(ServiceEntity entity, LatestVersionInfo latestVersion) {
        ServiceGroup group = entity.getGroup();
        GroupInfo groupInfo = group != null ? new GroupInfo(group.getId(), group.getName(), group.getIcon(), group.getColor()) : null;

        return new ServiceWithVersionDTO(
                entity.getId(),
                entity.getName(),
                entity.getOpenApiUrl(),
                entity.getDescription(),
                entity.isEnabled(),
                group != null ? group.getId() : null,
                groupInfo,
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                latestVersion
        );
    }

    public static ServiceWithVersionDTO fromEntity(ServiceEntity entity) {
        return fromEntity(entity, null);
    }

    public record GroupInfo(
            Long id,
            String name,
            String icon,
            String color
    ) {
    }

    public record LatestVersionInfo(
            Long id,
            String versionHash,
            String specVersion,
            String specTitle,
            Long fileSizeBytes,
            Instant pulledAt
    ) {
    }
}
