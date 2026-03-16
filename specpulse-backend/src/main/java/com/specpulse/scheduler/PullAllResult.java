package com.specpulse.scheduler;

public record PullAllResult(
        int newVersions,
        int unchanged,
        int failed
) {
}
