package com.specpulse.util;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for date/time operations
 */
public class DateUtils {

    private static final DateTimeFormatter DEFAULT_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final DateTimeFormatter ISO_FORMATTER =
            DateTimeFormatter.ISO_INSTANT;

    private DateUtils() {
        // Utility class
    }

    /**
     * Get current instant
     */
    public static Instant now() {
        return Instant.now();
    }

    /**
     * Format instant to local date time string
     */
    public static String formatInstant(Instant instant) {
        return formatInstant(instant, DEFAULT_FORMATTER);
    }

    /**
     * Format instant with custom formatter
     */
    public static String formatInstant(Instant instant, DateTimeFormatter formatter) {
        if (instant == null) {
            return null;
        }
        LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        return localDateTime.format(formatter);
    }

    /**
     * Parse date string to instant
     */
    public static Instant parseInstant(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }
        return Instant.parse(dateString);
    }

    /**
     * Check if instant is in the past
     */
    public static boolean isPast(Instant instant) {
        return instant != null && instant.isBefore(Instant.now());
    }

    /**
     * Check if instant is in the future
     */
    public static boolean isFuture(Instant instant) {
        return instant != null && instant.isAfter(Instant.now());
    }

    /**
     * Calculate duration between two instants
     */
    public static Duration durationBetween(Instant start, Instant end) {
        if (start == null || end == null) {
            return null;
        }
        return Duration.between(start, end);
    }

    /**
     * Get human-readable duration
     */
    public static String humanReadableDuration(Duration duration) {
        if (duration == null) {
            return "N/A";
        }

        long seconds = duration.getSeconds();
        long absSeconds = Math.abs(seconds);

        long hours = absSeconds / 3600;
        long minutes = (absSeconds % 3600) / 60;
        long secs = absSeconds % 60;

        StringBuilder result = new StringBuilder();
        if (hours > 0) {
            result.append(hours).append("h ");
        }
        if (minutes > 0 || hours > 0) {
            result.append(minutes).append("m ");
        }
        result.append(secs).append("s");

        return seconds < 0 ? "-" + result.toString() : result.toString();
    }

    /**
     * Check if instant is within last N days
     */
    public static boolean isWithinDays(Instant instant, int days) {
        if (instant == null) {
            return false;
        }
        Instant daysAgo = Instant.now().minus(Duration.ofDays(days));
        return instant.isAfter(daysAgo);
    }
}
