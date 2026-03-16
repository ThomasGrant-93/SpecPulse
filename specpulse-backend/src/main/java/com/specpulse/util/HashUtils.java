package com.specpulse.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Utility class for hashing operations
 */
public class HashUtils {

    private HashUtils() {
        // Utility class
    }

    /**
     * Compute SHA-256 hash of a string
     *
     * @param input the input string
     * @return hexadecimal hash string
     */
    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Compute MD5 hash of a string (not recommended for security)
     *
     * @param input the input string
     * @return hexadecimal hash string
     */
    public static String md5(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hashBytes = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }

    /**
     * Truncate hash to specified length
     *
     * @param hash   the full hash
     * @param length the desired length
     * @return truncated hash
     */
    public static String truncateHash(String hash, int length) {
        if (hash == null || hash.length() <= length) {
            return hash;
        }
        return hash.substring(0, length);
    }
}
