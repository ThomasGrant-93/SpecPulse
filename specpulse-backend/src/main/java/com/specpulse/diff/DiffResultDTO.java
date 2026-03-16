package com.specpulse.diff;

/**
 * DTO for diff comparison result
 */
public record DiffResultDTO(
        String diff,
        boolean hasBreakingChanges,
        int breakingChangesCount,
        boolean compatible,
        boolean incompatible,
        boolean unchanged
) {
}
