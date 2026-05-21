package com.bupt.tarecruit.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ValidationUtil blank checks.
 */
class ValidationUtilTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  \t  "})
    void isBlankShouldBeTrueForNullOrWhitespace(String value) {
        assertTrue(ValidationUtil.isBlank(value));
    }
    /** Verifies is blank should be false for non blank. */
    @Test
    void isBlankShouldBeFalseForNonBlank() {
        assertFalse(ValidationUtil.isBlank("hello"));
    }
    /** Verifies require non blank should throw with message. */
    @Test
    void requireNonBlankShouldThrowWithMessage() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ValidationUtil.requireNonBlank("  ", "Email is required"));
        assertEquals("Email is required", ex.getMessage());
    }
    /** Verifies require non blank should pass for valid value. */
    @Test
    void requireNonBlankShouldPassForValidValue() {
        assertDoesNotThrow(() -> ValidationUtil.requireNonBlank("ta@bupt.edu.cn", "ignored"));
    }
}
