package com.bupt.tarecruit.util;

/**
 * Validates BUPT International School TA/MO account identifiers.
 * TA: student number {@code ta} + 8 digits (first 4 = enrollment year).
 * MO: staff number {@code mo} + 8 digits (first 4 = hire year).
 * Administrator is always {@code admin} (not created via self-registration).
 */
public final class IdFormatUtil {

    private static final String TA_PATTERN = "^ta(19|20)\\d{2}\\d{4}$";
    private static final String MO_PATTERN = "^mo(19|20)\\d{2}\\d{4}$";

    private IdFormatUtil() {
    }

    public static boolean isValidTaStudentId(String id) {
        return id != null && id.matches(TA_PATTERN);
    }

    public static boolean isValidMoStaffId(String id) {
        return id != null && id.matches(MO_PATTERN);
    }

    public static boolean isAdminUserId(String id) {
        return id != null && "admin".equalsIgnoreCase(id.trim());
    }
}
