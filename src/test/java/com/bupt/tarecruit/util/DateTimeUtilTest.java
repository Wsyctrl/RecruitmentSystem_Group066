package com.bupt.tarecruit.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DateTimeUtil parsing and formatting.
 */
class DateTimeUtilTest {
    /** Verifies parse and format date round trip. */
    @Test
    void parseAndFormatDateRoundTrip() {
        LocalDate date = LocalDate.of(2025, 3, 15);
        assertEquals("2025-03-15", DateTimeUtil.formatDate(date));
        assertEquals(date, DateTimeUtil.parseDate("2025-03-15").orElseThrow());
    }
    /** Verifies parse date invalid or blank returns empty. */
    @Test
    void parseDateInvalidOrBlankReturnsEmpty() {
        assertTrue(DateTimeUtil.parseDate(null).isEmpty());
        assertTrue(DateTimeUtil.parseDate("").isEmpty());
        assertTrue(DateTimeUtil.parseDate("not-a-date").isEmpty());
        assertTrue(DateTimeUtil.parseDate("2025/03/15").isEmpty());
    }
    /** Verifies parse and format date time round trip. */
    @Test
    void parseAndFormatDateTimeRoundTrip() {
        LocalDateTime dt = LocalDateTime.of(2025, 3, 15, 14, 30);
        assertEquals("2025-03-15 14:30", DateTimeUtil.formatDateTime(dt));
        assertEquals(dt, DateTimeUtil.parseDateTime("2025-03-15 14:30").orElseThrow());
    }
    /** Verifies format null returns empty string. */
    @Test
    void formatNullReturnsEmptyString() {
        assertEquals("", DateTimeUtil.formatDate(null));
        assertEquals("", DateTimeUtil.formatDateTime(null));
    }
}
