package com.almousleck.utils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ReferenceGenerator {

    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyMMdd");

    private ReferenceGenerator() {
        // Private constructor to prevent instantiation
    }

    public static String generateBookingReference() {
        // Format: ANB-{date}-{random}
        // Example: ANB-230615-XY4Z
        String datePart = LocalDateTime.now().format(DATE_FORMAT);
        String randomPart = generateRandomString(4);
        return String.format("ANB-%s-%s", datePart, randomPart);
    }

    public static String generateTransactionReference() {
        // Format: TRX-{date}-{random}
        // Example: TRX-230615-AB12CD34
        String datePart = LocalDateTime.now().format(DATE_FORMAT);
        String randomPart = generateRandomString(8);
        return String.format("TRX-%s-%s", datePart, randomPart);
    }

    public static String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }
}

