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

    // Information about nested groups
    private List<ServiceGroupDTO> childGroups;

    // Number of services in the group
    private Integer serviceCount;

    // Services in the group (optional, for detailed view)
    private List<GroupServiceDTO> services;

    /**
     * DTO for a service inside a group
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
