package com.specpulse.group;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateGroupRequest {

    @Size(max = 255, message = "Group name must be less than 255 characters")
    private String name;

    @Size(max = 1000, message = "Description must be less than 1000 characters")
    private String description;

    private Long parentGroupId;

    @Size(max = 7, message = "Color must be a hex color code")
    private String color;

    @Size(max = 50, message = "Icon must be less than 50 characters")
    private String icon;

    private Integer sortOrder;
}
