package com.specpulse.registry;

import com.specpulse.entity.ServiceGroup;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

public record ServiceDTO(
        Long id,
        String name,
        String openApiUrl,
        String description,
        boolean enabled,
        Long groupId,
        GroupInfo group
) {
    @Contract("_ -> new")
    public static @NonNull ServiceDTO fromEntity(ServiceEntity entity) {
        ServiceGroup group = entity.getGroup();
        GroupInfo groupInfo = group != null ? new GroupInfo(group.getId(), group.getName(), group.getIcon(), group.getColor()) : null;

        return new ServiceDTO(
                entity.getId(),
                entity.getName(),
                entity.getOpenApiUrl(),
                entity.getDescription(),
                entity.isEnabled(),
                group != null ? group.getId() : null,
                groupInfo
        );
    }

    public record GroupInfo(
            Long id,
            String name,
            String icon,
            String color
    ) {
    }
}
