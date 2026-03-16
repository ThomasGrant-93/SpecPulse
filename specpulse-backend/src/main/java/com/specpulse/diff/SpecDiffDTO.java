package com.specpulse.diff;

import java.time.Instant;

public record SpecDiffDTO(
        Long id,
        Long serviceId,
        Long fromVersionId,
        Long toVersionId,
        String diffContent,
        boolean hasBreakingChanges,
        int breakingChangesCount,
        Instant createdAt
) {
    public static SpecDiffDTO fromEntity(SpecDiffEntity entity) {
        return new SpecDiffDTO(
                entity.getId(),
                entity.getServiceId(),
                entity.getFromVersionId(),
                entity.getToVersionId(),
                entity.getDiffContent(),
                entity.isHasBreakingChanges(),
                entity.getBreakingChangesCount(),
                entity.getCreatedAt()
        );
    }
}
