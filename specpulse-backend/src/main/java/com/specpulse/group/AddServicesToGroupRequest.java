package com.specpulse.group;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddServicesToGroupRequest {

    @NotNull(message = "Group ID is required")
    private Long groupId;

    @NotEmpty(message = "At least one service ID is required")
    private List<Long> serviceIds;
}
