package com.specpulse.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HashUtilsTest {

    @Test
    @DisplayName("Should generate consistent SHA-256 hash")
    void shouldGenerateConsistentSha256Hash() {
        // Given
        String input = "test-input";

        // When
        String hash1 = HashUtils.sha256(input);
        String hash2 = HashUtils.sha256(input);

        // Then
        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).hasSize(64); // SHA-256 produces 64 hex characters
    }

    @Test
    @DisplayName("Should generate different hashes for different inputs")
    void shouldGenerateDifferentHashesForDifferentInputs() {
        // Given
        String input1 = "input-1";
        String input2 = "input-2";

        // When
        String hash1 = HashUtils.sha256(input1);
        String hash2 = HashUtils.sha256(input2);

        // Then
        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    @DisplayName("Should handle empty string")
    void shouldHandleEmptyString() {
        // When
        String hash = HashUtils.sha256("");

        // Then
        assertThat(hash).hasSize(64);
        assertThat(hash).isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }

    @Test
    @DisplayName("Should throw exception for null input")
    void shouldThrowExceptionForNullInput() {
        // When & Then
        assertThatThrownBy(() -> HashUtils.sha256(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should truncate hash to specified length")
    void shouldTruncateHash() {
        // Given
        String hash = HashUtils.sha256("test");

        // When
        String truncated = HashUtils.truncateHash(hash, 8);

        // Then
        assertThat(truncated).hasSize(8);
        assertThat(hash).startsWith(truncated);
    }

    @Test
    @DisplayName("Should return original hash when length is greater than hash")
    void shouldReturnOriginalHashWhenLengthGreaterThanHash() {
        // Given
        String hash = HashUtils.sha256("test");

        // When
        String result = HashUtils.truncateHash(hash, 100);

        // Then
        assertThat(result).isEqualTo(hash);
    }

    @Test
    @DisplayName("Should handle null hash in truncate")
    void shouldHandleNullHashInTruncate() {
        // When
        String result = HashUtils.truncateHash(null, 8);

        // Then
        assertThat(result).isNull();
    }
}
