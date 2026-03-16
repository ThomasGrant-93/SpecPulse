package com.specpulse.client;

public record SpecFetchResult(
        boolean success,
        String content,
        Integer httpStatusCode,
        String errorMessage,
        long durationMs
) {
    public static SpecFetchResult success(String content, Integer httpStatusCode, long durationMs) {
        return new SpecFetchResult(true, content, httpStatusCode, null, durationMs);
    }

    public static SpecFetchResult failure(Integer httpStatusCode, String errorMessage, long durationMs) {
        return new SpecFetchResult(false, null, httpStatusCode, errorMessage, durationMs);
    }

    public static SpecFetchResult error(String errorMessage, long durationMs) {
        return new SpecFetchResult(false, null, null, errorMessage, durationMs);
    }
}
