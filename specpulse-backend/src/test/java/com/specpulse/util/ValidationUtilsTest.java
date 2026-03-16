package com.specpulse.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidationUtilsTest {

    @Test
    @DisplayName("Should return true for non-blank string")
    void shouldReturnTrueForNonBlankString() {
        // When
        boolean result = ValidationUtils.isNotBlank("  valid  ");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return false for blank string")
    void shouldReturnFalseForBlankString() {
        // When
        boolean result = ValidationUtils.isBlank("   ");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should return true for null or empty collection")
    void shouldReturnTrueForNullOrEmptyCollection() {
        // When
        boolean nullResult = ValidationUtils.isEmpty((java.util.Collection<?>) null);
        boolean emptyResult = ValidationUtils.isEmpty(java.util.List.of());

        // Then
        assertThat(nullResult).isTrue();
        assertThat(emptyResult).isTrue();
    }

    @Test
    @DisplayName("Should return false for non-empty collection")
    void shouldReturnFalseForNonEmptyCollection() {
        // When
        boolean result = ValidationUtils.isEmpty(java.util.List.of("item"));

        // Then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should require non-null value")
    void shouldRequireNonNullValue() {
        // When & Then
        assertThat(ValidationUtils.requireNonNull("value", "message"))
                .isEqualTo("value");

        assertThatThrownBy(() -> ValidationUtils.requireNonNull(null, "message"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("message");
    }

    @Test
    @DisplayName("Should require non-blank string")
    void shouldRequireNonBlankString() {
        // When & Then
        assertThat(ValidationUtils.requireNonBlank("  valid  ", "message"))
                .isEqualTo("  valid  ");

        assertThatThrownBy(() -> ValidationUtils.requireNonBlank("   ", "message"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("message");

        assertThatThrownBy(() -> ValidationUtils.requireNonBlank(null, "message"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("message");
    }

    @ParameterizedTest
    @ValueSource(strings = {"test@example.com", "user.name@domain.org", "admin@test.co.uk"})
    @DisplayName("Should validate valid email addresses")
    void shouldValidateValidEmails(String email) {
        // When
        boolean result = ValidationUtils.isValidEmail(email);

        // Then
        assertThat(result).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid", "@no-local.com", "no-at-sign.com", ""})
    @DisplayName("Should invalidate invalid email addresses")
    void shouldInvalidateInvalidEmails(String email) {
        // When
        boolean result = ValidationUtils.isValidEmail(email);

        // Then
        assertThat(result).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://example.com",
            "https://api.example.com/path",
            "http://localhost:8080/api"
    })
    @DisplayName("Should validate valid URLs")
    void shouldValidateValidUrls(String url) {
        // When
        boolean result = ValidationUtils.isValidUrl(url);

        // Then
        assertThat(result).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ftp://example.com", "example.com", "htp://invalid", ""})
    @DisplayName("Should invalidate invalid URLs")
    void shouldInvalidateInvalidUrls(String url) {
        // When
        boolean result = ValidationUtils.isValidUrl(url);

        // Then
        assertThat(result).isFalse();
    }
}
