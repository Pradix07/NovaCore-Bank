package com.bank.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.UUID;

public class SecurityUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Hashes password using SHA-256.
     */
    public static String hashPassword(String password) {
        if (password == null) return "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    public static boolean verifyPassword(String rawPassword, String hashedPassword) {
        return hashPassword(rawPassword).equalsIgnoreCase(hashedPassword);
    }

    public static String generateId(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public static String generateAccountNumber(String accountType) {
        String prefix = "SAVINGS".equalsIgnoreCase(accountType) ? "100" : "200";
        int randomDigits = 1000000 + RANDOM.nextInt(9000000);
        return prefix + randomDigits;
    }

    public static String generateCardNumber() {
        long part1 = 453200000000L + RANDOM.nextLong(100000000L);
        return String.format("%016d", part1);
    }

    public static String generateCardExpiry() {
        return "12/29";
    }

    public static String generateCvv() {
        return String.format("%03d", RANDOM.nextInt(1000));
    }

    public static String generateReferenceNumber() {
        return "TXN" + System.currentTimeMillis() + (100 + RANDOM.nextInt(900));
    }
}
