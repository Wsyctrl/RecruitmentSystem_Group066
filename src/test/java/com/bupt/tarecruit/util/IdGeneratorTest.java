package com.bupt.tarecruit.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IdGeneratorTest {

    @Test
    void nextIdStartsAt001WhenEmpty() {
        assertEquals("job001", IdGenerator.nextId("job", List.of()));
    }

    @Test
    void nextIdIncrementsFromMaxExisting() {
        List<String> existing = List.of("apply001", "apply003", "other999");
        assertEquals("apply004", IdGenerator.nextId("apply", existing));
    }

    @Test
    void nextIdIgnoresNonMatchingPrefix() {
        assertEquals("alog001", IdGenerator.nextId("alog", List.of("job005", "xyz")));
    }
}
