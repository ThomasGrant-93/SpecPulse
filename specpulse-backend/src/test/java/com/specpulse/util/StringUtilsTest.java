package com.specpulse.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StringUtilsTest {

    @Test
    @DisplayName("Should convert camelCase to snake_case")
    void shouldConvertCamelToSnake() {
        // When
        String result = StringUtils.camelToSnake("camelCaseString");

        // Then
        assertThat(result).isEqualTo("camel_case_string");
    }

    @Test
    @DisplayName("Should handle null in camelToSnake")
    void shouldHandleNullInCamelToSnake() {
        // When
        String result = StringUtils.camelToSnake(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should convert snake_case to camelCase")
    void shouldConvertSnakeToCamel() {
        // When
        String result = StringUtils.snakeToCamel("snake_case_string");

        // Then
        assertThat(result).isEqualTo("snakeCaseString");
    }

    @Test
    @DisplayName("Should handle null in snakeToCamel")
    void shouldHandleNullInSnakeToCamel() {
        // When
        String result = StringUtils.snakeToCamel(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should truncate string with ellipsis")
    void shouldTruncateStringWithEllipsis() {
        // When
        String result = StringUtils.truncate("Hello World", 8);

        // Then
        assertThat(result).isEqualTo("Hello...");
    }

    @Test
    @DisplayName("Should not truncate if string is shorter")
    void shouldNotTruncateIfShorter() {
        // When
        String result = StringUtils.truncate("Hi", 10);

        // Then
        assertThat(result).isEqualTo("Hi");
    }

    @Test
    @DisplayName("Should handle null in truncate")
    void shouldHandleNullInTruncate() {
        // When
        String result = StringUtils.truncate(null, 10);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should remove whitespace")
    void shouldRemoveWhitespace() {
        // When
        String result = StringUtils.removeWhitespace("Hello World Test");

        // Then
        assertThat(result).isEqualTo("HelloWorldTest");
    }

    @Test
    @DisplayName("Should keep only digits")
    void shouldKeepDigitsOnly() {
        // When
        String result = StringUtils.keepDigitsOnly("abc123def456");

        // Then
        assertThat(result).isEqualTo("123456");
    }

    @Test
    @DisplayName("Should keep only alphanumeric")
    void shouldKeepAlphanumericOnly() {
        // When
        String result = StringUtils.keepAlphanumericOnly("Hello@World#123!");

        // Then
        assertThat(result).isEqualTo("HelloWorld123");
    }

    @Test
    @DisplayName("Should capitalize first letter")
    void shouldCapitalizeFirstLetter() {
        // When
        String result = StringUtils.capitalize("hello");

        // Then
        assertThat(result).isEqualTo("Hello");
    }

    @Test
    @DisplayName("Should uncapitalize first letter")
    void shouldUncapitalizeFirstLetter() {
        // When
        String result = StringUtils.uncapitalize("Hello");

        // Then
        assertThat(result).isEqualTo("hello");
    }

    @Test
    @DisplayName("Should reverse string")
    void shouldReverseString() {
        // When
        String result = StringUtils.reverse("hello");

        // Then
        assertThat(result).isEqualTo("olleh");
    }

    @Test
    @DisplayName("Should count occurrences")
    void shouldCountOccurrences() {
        // When
        int result = StringUtils.countOccurrences("hello world hello", "hello");

        // Then
        assertThat(result).isEqualTo(2);
    }

    @Test
    @DisplayName("Should check if numeric")
    void shouldCheckIfNumeric() {
        // When
        boolean result = StringUtils.isNumeric("12345");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should check if alpha")
    void shouldCheckIfAlpha() {
        // When
        boolean result = StringUtils.isAlpha("hello");

        // Then
        assertThat(result).isTrue();
    }
}
