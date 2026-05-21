package com.bupt.tarecruit.util;

import java.util.Collection;

/**
 * Generates sequential string identifiers with a fixed prefix and zero-padded numeric suffix.
 */
public final class IdGenerator {

    private IdGenerator() {
    }

    /**
     * Returns the next identifier after the highest existing ID with the same prefix.
     * <p>
     * Numeric suffixes are three digits (e.g. {@code JOB001}, {@code JOB002}). IDs that
     * do not match {@code prefix} followed by digits are ignored when computing the maximum.
     * </p>
     *
     * @param prefix      non-null prefix shared by IDs in the collection (e.g. {@code "JOB"})
     * @param existingIds collection of existing identifier strings
     * @return new identifier one greater than the current maximum
     */
    public static String nextId(String prefix, Collection<String> existingIds) {
        int maxNumber = existingIds.stream()
                .filter(id -> id != null && id.startsWith(prefix))
                .map(id -> id.replace(prefix, ""))
                .filter(part -> part.matches("\\d+"))
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(0);
        return prefix + String.format("%03d", maxNumber + 1);
    }
}
