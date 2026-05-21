package com.bupt.tarecruit.util;

/**
 * Common string validation helpers for forms and service input checks.
 */
public final class ValidationUtil {

    private ValidationUtil() {
    }

    /**
     * Returns whether the value is null, empty, or whitespace only.
     *
     * @param value text to check
     * @return {@code true} when the value is considered blank
     */
    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * Ensures the value is not blank.
     *
     * @param value   text to validate
     * @param message exception message when validation fails
     * @throws IllegalArgumentException when the value is blank
     */
    public static void requireNonBlank(String value, String message) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(message);
        }
    }
}
