package com.bupt.tarecruit.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class ValidationUtilTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  \t  "})
    void isBlankShouldBeTrueForNullOrWhitespace(String value) {
        assertTrue(ValidationUtil.isBlank(value));
    }

    @Test
    void isBlankShouldBeFalseForNonBlank() {
        assertFalse(ValidationUtil.isBlank("hello"));
    }

    @Test
    void requireNonBlankShouldThrowWithMessage() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ValidationUtil.requireNonBlank("  ", "Email is required"));
        assertEquals("Email is required", ex.getMessage());
    }

    @Test
    void requireNonBlankShouldPassForValidValue() {
        assertDoesNotThrow(() -> ValidationUtil.requireNonBlank("ta@bupt.edu.cn", "ignored"));
    }
}
