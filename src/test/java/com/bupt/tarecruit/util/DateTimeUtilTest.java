package com.bupt.tarecruit.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DateTimeUtilTest {

    @Test
    void parseAndFormatDateRoundTrip() {
        LocalDate date = LocalDate.of(2025, 3, 15);
        assertEquals("2025-03-15", DateTimeUtil.formatDate(date));
        assertEquals(date, DateTimeUtil.parseDate("2025-03-15").orElseThrow());
    }

    @Test
    void parseDateInvalidOrBlankReturnsEmpty() {
        assertTrue(DateTimeUtil.parseDate(null).isEmpty());
        assertTrue(DateTimeUtil.parseDate("").isEmpty());
        assertTrue(DateTimeUtil.parseDate("not-a-date").isEmpty());
        assertTrue(DateTimeUtil.parseDate("2025/03/15").isEmpty());
    }

    @Test
    void parseAndFormatDateTimeRoundTrip() {
        LocalDateTime dt = LocalDateTime.of(2025, 3, 15, 14, 30);
        assertEquals("2025-03-15 14:30", DateTimeUtil.formatDateTime(dt));
        assertEquals(dt, DateTimeUtil.parseDateTime("2025-03-15 14:30").orElseThrow());
    }

    @Test
    void formatNullReturnsEmptyString() {
        assertEquals("", DateTimeUtil.formatDate(null));
        assertEquals("", DateTimeUtil.formatDateTime(null));
    }
}
