package com.specpulse.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class DateUtilsTest {

    @Test
    @DisplayName("Should return current instant")
    void shouldReturnNowInstant() {
        // When
        Instant now = DateUtils.now();

        // Then
        assertThat(now).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    @DisplayName("Should format instant to string")
    void shouldFormatInstantToString() {
        // Given
        Instant instant = Instant.parse("2024-01-15T10:30:00Z");

        // When
        String result = DateUtils.formatInstant(instant);

        // Then
        assertThat(result).contains("2024-01-15");
    }

    @Test
    @DisplayName("Should handle null instant")
    void shouldHandleNullInstant() {
        // When
        String result = DateUtils.formatInstant(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should parse instant from ISO string")
    void shouldParseInstantFromIsoString() {
        // Given
        String dateString = "2024-01-15T10:30:00Z";

        // When
        Instant result = DateUtils.parseInstant(dateString);

        // Then
        assertThat(result).isEqualTo(Instant.parse("2024-01-15T10:30:00Z"));
    }

    @Test
    @DisplayName("Should handle null in parseInstant")
    void shouldHandleNullInParseInstant() {
        // When
        Instant result = DateUtils.parseInstant(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should check if instant is in past")
    void shouldCheckIfInstantIsInPast() {
        // Given
        Instant past = Instant.now().minusSeconds(10);

        // When
        boolean result = DateUtils.isPast(past);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should check if instant is in future")
    void shouldCheckIfInstantIsInFuture() {
        // Given
        Instant future = Instant.now().plusSeconds(10);

        // When
        boolean result = DateUtils.isFuture(future);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should calculate duration between instants")
    void shouldCalculateDurationBetweenInstants() {
        // Given
        Instant start = Instant.parse("2024-01-15T10:00:00Z");
        Instant end = Instant.parse("2024-01-15T10:05:00Z");

        // When
        Duration result = DateUtils.durationBetween(start, end);

        // Then
        assertThat(result.toMinutes()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should format duration in human readable format")
    void shouldFormatDurationHumanReadable() {
        // Given
        Duration duration = Duration.ofMinutes(95); // 1h 35m

        // When
        String result = DateUtils.humanReadableDuration(duration);

        // Then
        assertThat(result).contains("1h");
        assertThat(result).contains("35m");
    }

    @Test
    @DisplayName("Should handle null duration")
    void shouldHandleNullDuration() {
        // When
        String result = DateUtils.humanReadableDuration(null);

        // Then
        assertThat(result).isEqualTo("N/A");
    }

    @Test
    @DisplayName("Should check if instant is within days")
    void shouldCheckIfInstantIsWithinDays() {
        // Given
        Instant now = Instant.now();
        Instant twoDaysAgo = now.minus(Duration.ofDays(2));

        // When
        boolean result = DateUtils.isWithinDays(twoDaysAgo, 3);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false for instant older than days")
    void shouldReturnFalseForOldInstant() {
        // Given
        Instant tenDaysAgo = Instant.now().minus(Duration.ofDays(10));

        // When
        boolean result = DateUtils.isWithinDays(tenDaysAgo, 3);

        // Then
        assertThat(result).isFalse();
    }
}
