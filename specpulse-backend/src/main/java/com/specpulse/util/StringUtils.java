package com.specpulse.util;

import java.util.regex.Pattern;

/**
 * Utility class for string operations
 */
public class StringUtils {

    private static final Pattern CAMEL_CASE_PATTERN = Pattern.compile(
            "([a-z])([A-Z]+)"
    );

    private static final Pattern SNAKE_CASE_PATTERN = Pattern.compile(
            "([a-z0-9])([A-Z])"
    );

    private StringUtils() {
        // Utility class
    }

    /**
     * Convert camelCase to snake_case
     */
    public static String camelToSnake(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return camelCase;
        }
        return SNAKE_CASE_PATTERN.matcher(camelCase)
                .replaceAll("$1_$2")
                .toLowerCase();
    }

    /**
     * Convert snake_case to camelCase
     */
    public static String snakeToCamel(String snakeCase) {
        if (snakeCase == null || snakeCase.isEmpty()) {
            return snakeCase;
        }

        StringBuilder result = new StringBuilder();
        boolean nextUpper = false;

        for (char c : snakeCase.toCharArray()) {
            if (c == '_') {
                nextUpper = true;
            } else {
                result.append(nextUpper ? Character.toUpperCase(c) : c);
                nextUpper = false;
            }
        }

        return result.toString();
    }

    /**
     * Truncate string to max length with ellipsis
     */
    public static String truncate(String str, int maxLength) {
        return truncate(str, maxLength, "...");
    }

    /**
     * Truncate string to max length with custom suffix
     */
    public static String truncate(String str, int maxLength, String suffix) {
        if (str == null || str.length() <= maxLength) {
            return str;
        }
        if (maxLength <= suffix.length()) {
            return suffix.substring(0, maxLength);
        }
        return str.substring(0, maxLength - suffix.length()) + suffix;
    }

    /**
     * Remove all whitespace from string
     */
    public static String removeWhitespace(String str) {
        if (str == null) {
            return null;
        }
        return str.replaceAll("\\s+", "");
    }

    /**
     * Remove all non-digit characters
     */
    public static String keepDigitsOnly(String str) {
        if (str == null) {
            return null;
        }
        return str.replaceAll("\\D+", "");
    }

    /**
     * Remove all non-alphanumeric characters
     */
    public static String keepAlphanumericOnly(String str) {
        if (str == null) {
            return null;
        }
        return str.replaceAll("[^a-zA-Z0-9]", "");
    }

    /**
     * Capitalize first letter
     */
    public static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    /**
     * Uncapitalize first letter
     */
    public static String uncapitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }

    /**
     * Reverse string
     */
    public static String reverse(String str) {
        if (str == null) {
            return null;
        }
        return new StringBuilder(str).reverse().toString();
    }

    /**
     * Count occurrences of substring
     */
    public static int countOccurrences(String str, String sub) {
        if (str == null || sub == null || str.isEmpty() || sub.isEmpty()) {
            return 0;
        }

        int count = 0;
        int idx = 0;

        while ((idx = str.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }

        return count;
    }

    /**
     * Check if string contains only digits
     */
    public static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        return str.chars().allMatch(Character::isDigit);
    }

    /**
     * Check if string contains only letters
     */
    public static boolean isAlpha(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        return str.chars().allMatch(Character::isLetter);
    }
}
