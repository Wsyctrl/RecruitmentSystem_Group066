package com.bupt.tarecruit.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for IdGenerator monotonic ID generation.
 */
class IdGeneratorTest {
    /** Verifies next id starts at001 when empty. */
    @Test
    void nextIdStartsAt001WhenEmpty() {
        assertEquals("job001", IdGenerator.nextId("job", List.of()));
    }
    /** Verifies next id increments from max existing. */
    @Test
    void nextIdIncrementsFromMaxExisting() {
        List<String> existing = List.of("apply001", "apply003", "other999");
        assertEquals("apply004", IdGenerator.nextId("apply", existing));
    }
    /** Verifies next id ignores non matching prefix. */
    @Test
    void nextIdIgnoresNonMatchingPrefix() {
        assertEquals("alog001", IdGenerator.nextId("alog", List.of("job005", "xyz")));
    }
}
