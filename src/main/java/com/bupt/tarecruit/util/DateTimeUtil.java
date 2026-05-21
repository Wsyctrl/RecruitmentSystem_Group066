package com.bupt.tarecruit.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/**
 * Parses and formats dates and date-times used in CSV fields and the UI.
 * <p>
 * Supported patterns: {@code yyyy-MM-dd} for dates and
 * {@code yyyy-MM-dd HH:mm} for date-times. Unparseable or blank input yields
 * {@link Optional#empty()} for parsing methods and an empty string for formatting
 * {@code null} values.
 * </p>
 */
public final class DateTimeUtil {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private DateTimeUtil() {
    }

    /**
     * Parses a date string in {@code yyyy-MM-dd} format.
     *
     * @param value text to parse; {@code null} or blank yields empty
     * @return parsed date, or empty when the value is missing or invalid
     */
    public static Optional<LocalDate> parseDate(String value) {
        try {
            return value == null || value.isBlank() ? Optional.empty() : Optional.of(LocalDate.parse(value, DATE_FORMATTER));
        } catch (DateTimeParseException ex) {
            return Optional.empty();
        }
    }

    /**
     * Parses a date-time string in {@code yyyy-MM-dd HH:mm} format.
     *
     * @param value text to parse; {@code null} or blank yields empty
     * @return parsed date-time, or empty when the value is missing or invalid
     */
    public static Optional<LocalDateTime> parseDateTime(String value) {
        try {
            return value == null || value.isBlank() ? Optional.empty() : Optional.of(LocalDateTime.parse(value, DATE_TIME_FORMATTER));
        } catch (DateTimeParseException ex) {
            return Optional.empty();
        }
    }

    /**
     * Formats a date for CSV storage or display.
     *
     * @param date date to format; {@code null} yields an empty string
     * @return formatted date text
     */
    public static String formatDate(LocalDate date) {
        return date == null ? "" : DATE_FORMATTER.format(date);
    }

    /**
     * Formats a date-time for CSV storage or display.
     *
     * @param dateTime date-time to format; {@code null} yields an empty string
     * @return formatted date-time text
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "" : DATE_TIME_FORMATTER.format(dateTime);
    }
}
