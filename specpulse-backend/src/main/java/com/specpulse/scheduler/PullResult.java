package com.specpulse.scheduler;

public record PullResult(
        boolean success,
        boolean hasChanges,
        Long newVersionId,
        Long previousVersionId,
        String versionHash,
        String error
) {
    public static PullResult success(boolean hasChanges, Long newVersionId,
                                     Long previousVersionId, String versionHash) {
        return new PullResult(true, hasChanges, newVersionId, previousVersionId, versionHash, null);
    }

    public static PullResult failed(String error) {
        return new PullResult(false, false, null, null, null, error);
    }
}
