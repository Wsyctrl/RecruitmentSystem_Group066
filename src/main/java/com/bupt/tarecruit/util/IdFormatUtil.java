package com.bupt.tarecruit.util;

/**
 * Validates account identity values.
 * <p>
 * All users (TA, MO, and Admin) use {@code @bupt.edu.cn} email addresses as identity.
 * </p>
 */
public final class IdFormatUtil {

    /** Regular expression for valid BUPT email addresses used as user identifiers. */
    private static final String BUPT_EMAIL_PATTERN = "^[A-Za-z0-9._%+-]+@bupt\\.edu\\.cn$";

    private IdFormatUtil() {
    }

    /**
     * Checks whether the value is a valid BUPT email address.
     *
     * @param email email text to validate
     * @return {@code true} when the value matches the BUPT email pattern
     */
    public static boolean isValidBuptEmail(String email) {
        return email != null && email.matches(BUPT_EMAIL_PATTERN);
    }

    /**
     * Checks whether the email identifies the built-in administrator account.
     *
     * @param email email text to check
     * @return {@code true} for {@code admin@bupt.edu.cn} (case-insensitive)
     */
    public static boolean isAdminUserId(String email) {
        return "admin@bupt.edu.cn".equalsIgnoreCase(email == null ? null : email.trim());
    }

    /**
     * Validates a teaching assistant identity (BUPT email).
     * Backward-compatible alias for {@link #isValidBuptEmail(String)}.
     *
     * @param id identity value to validate
     * @return {@code true} when the value is a valid BUPT email
     */
    public static boolean isValidTaStudentId(String id) {
        return isValidBuptEmail(id);
    }

    /**
     * Validates a module organizer identity (BUPT email).
     * Backward-compatible alias for {@link #isValidBuptEmail(String)}.
     *
     * @param id identity value to validate
     * @return {@code true} when the value is a valid BUPT email
     */
    public static boolean isValidMoStaffId(String id) {
        return isValidBuptEmail(id);
    }
}
