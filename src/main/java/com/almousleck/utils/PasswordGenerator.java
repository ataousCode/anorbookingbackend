package com.almousleck.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.security.SecureRandom;

/**
 * Utility class for generating secure passwords and encoding them.
 * This is primarily used for development and testing purposes.
 */
public class PasswordGenerator {

    private static final String CHAR_LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String CHAR_UPPER = CHAR_LOWER.toUpperCase();
    private static final String NUMBER = "0123456789";
    private static final String SPECIAL_CHARS = "!@#$%^&*()_-+=<>?";
    private static final String ALL_CHARS = CHAR_LOWER + CHAR_UPPER + NUMBER + SPECIAL_CHARS;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private PasswordGenerator() {
        // Private constructor to prevent instantiation
    }

    /**
     * Generates a secure random password with the specified length.
     * The password will contain at least one lowercase letter, one uppercase letter,
     * one number, and one special character.
     *
     * @param length the length of the password to generate
     * @return the generated password
     */
    public static String generateSecurePassword(int length) {
        if (length < 8) {
            throw new IllegalArgumentException("Password length must be at least 8 characters");
        }

        StringBuilder password = new StringBuilder(length);

        // Add at least one of each required character type
        password.append(CHAR_LOWER.charAt(RANDOM.nextInt(CHAR_LOWER.length())));
        password.append(CHAR_UPPER.charAt(RANDOM.nextInt(CHAR_UPPER.length())));
        password.append(NUMBER.charAt(RANDOM.nextInt(NUMBER.length())));
        password.append(SPECIAL_CHARS.charAt(RANDOM.nextInt(SPECIAL_CHARS.length())));

        // Fill the rest with random characters
        for (int i = 4; i < length; i++) {
            password.append(ALL_CHARS.charAt(RANDOM.nextInt(ALL_CHARS.length())));
        }

        // Shuffle the password
        char[] passwordArray = password.toString().toCharArray();
        for (int i = 0; i < passwordArray.length; i++) {
            int randomIndex = RANDOM.nextInt(passwordArray.length);
            char temp = passwordArray[i];
            passwordArray[i] = passwordArray[randomIndex];
            passwordArray[randomIndex] = temp;
        }

        return new String(passwordArray);
    }

    /**
     * Encodes a password using BCrypt.
     *
     * @param rawPassword the raw password to encode
     * @return the encoded password
     */
    public static String encodePassword(String rawPassword) {
        return ENCODER.encode(rawPassword);
    }

    /**
     * Main method for testing and generating encoded passwords.
     * This can be used to generate passwords for database initialization.
     */
    public static void main(String[] args) {
        String password = "admin123";
        String encodedPassword = encodePassword(password);
        System.out.println("Raw password: " + password);
        System.out.println("Encoded password: " + encodedPassword);

        String generatedPassword = generateSecurePassword(12);
        String encodedGeneratedPassword = encodePassword(generatedPassword);
        System.out.println("\nGenerated password: " + generatedPassword);
        System.out.println("Encoded generated password: " + encodedGeneratedPassword);
    }
}

