package com.specpulse.version;

public record SpecVersionPullResult(
        boolean hasChanges,
        Long newVersionId,
        Long previousVersionId,
        String versionHash
) {
    public static SpecVersionPullResult unchanged(Long versionId, String hash) {
        return new SpecVersionPullResult(false, versionId, null, hash);
    }

    public static SpecVersionPullResult newVersion(Long newVersionId, String hash, Long previousVersionId) {
        return new SpecVersionPullResult(true, newVersionId, previousVersionId, hash);
    }
}
