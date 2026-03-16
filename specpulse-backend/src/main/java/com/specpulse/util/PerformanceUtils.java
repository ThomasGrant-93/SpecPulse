package com.specpulse.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * Utility class for performance measurement
 */
public class PerformanceUtils {

    private static final Logger log = LoggerFactory.getLogger(PerformanceUtils.class);

    private PerformanceUtils() {
        // Utility class
    }

    /**
     * Measure execution time of a supplier
     *
     * @param operation the operation to measure
     * @param <T>       the result type
     * @return TimedResult with result and duration
     */
    public static <T> TimedResult<T> measure(Supplier<T> operation) {
        long startTime = System.nanoTime();
        T result = operation.get();
        long durationNs = System.nanoTime() - startTime;
        return new TimedResult<>(result, durationNs);
    }

    /**
     * Measure execution time of a runnable
     *
     * @param operation the operation to measure
     * @return duration in nanoseconds
     */
    public static long measure(Runnable operation) {
        long startTime = System.nanoTime();
        operation.run();
        return System.nanoTime() - startTime;
    }

    /**
     * Measure and log execution time
     *
     * @param operation     the operation to measure
     * @param operationName name for logging
     * @param <T>           the result type
     * @return the result
     */
    public static <T> T measureAndLog(Supplier<T> operation, String operationName) {
        long startTime = System.nanoTime();
        T result = operation.get();
        long durationMs = (System.nanoTime() - startTime) / 1_000_000;
        log.debug("{} completed in {}ms", operationName, durationMs);
        return result;
    }

    /**
     * Timed result holder
     */
    public static class TimedResult<T> {
        private final T result;
        private final long durationNanos;

        public TimedResult(T result, long durationNanos) {
            this.result = result;
            this.durationNanos = durationNanos;
        }

        public T getResult() {
            return result;
        }

        public long getDurationNanos() {
            return durationNanos;
        }

        public long getDurationMillis() {
            return durationNanos / 1_000_000;
        }

        public double getDurationSeconds() {
            return durationNanos / 1_000_000_000.0;
        }
    }
}
