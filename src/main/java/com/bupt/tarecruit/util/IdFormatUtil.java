package com.bupt.tarecruit.util;

/**
 * Validates account identity values.
 * All users (TA/MO/Admin) now use {@code @bupt.edu.cn} email addresses as identity.
 */
public final class IdFormatUtil {

    private static final String BUPT_EMAIL_PATTERN = "^[A-Za-z0-9._%+-]+@bupt\\.edu\\.cn$";

    private IdFormatUtil() {
    }

    public static boolean isValidBuptEmail(String email) {
        return email != null && email.matches(BUPT_EMAIL_PATTERN);
    }

    public static boolean isAdminUserId(String email) {
        return "admin@bupt.edu.cn".equalsIgnoreCase(email == null ? null : email.trim());
    }

    // Backward-compatible wrappers for existing call sites.
    public static boolean isValidTaStudentId(String id) {
        return isValidBuptEmail(id);
    }

    public static boolean isValidMoStaffId(String id) {
        return isValidBuptEmail(id);
    }
}
