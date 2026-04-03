package com.bupt.tarecruit.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

public final class DateTimeUtil {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private DateTimeUtil() {
    }

    public static Optional<LocalDate> parseDate(String value) {
        try {
            return value == null || value.isBlank() ? Optional.empty() : Optional.of(LocalDate.parse(value, DATE_FORMATTER));
        } catch (DateTimeParseException ex) {
            return Optional.empty();
        }
    }

    public static Optional<LocalDateTime> parseDateTime(String value) {
        try {
            return value == null || value.isBlank() ? Optional.empty() : Optional.of(LocalDateTime.parse(value, DATE_TIME_FORMATTER));
        } catch (DateTimeParseException ex) {
            return Optional.empty();
        }
    }

    public static String formatDate(LocalDate date) {
        return date == null ? "" : DATE_FORMATTER.format(date);
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "" : DATE_TIME_FORMATTER.format(dateTime);
    }
}
