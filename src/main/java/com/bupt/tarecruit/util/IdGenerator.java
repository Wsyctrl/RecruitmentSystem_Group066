package com.bupt.tarecruit.util;

import java.util.Collection;

public final class IdGenerator {

    private IdGenerator() {
    }

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
