package com.bupt.tarecruit.dao;

import com.bupt.tarecruit.entity.Ta;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CsvTaDaoTest {

    @TempDir
    Path tempDir;

    @Test
    void saveAndUpdateTaRecord() {
        Path file = tempDir.resolve("TA.csv");
        CsvTaDao dao = new CsvTaDao(file);

        Ta ta = new Ta("test@bupt.edu.cn", "Secret@1");
        ta.setEmail("test@bupt.edu.cn");
        dao.save(ta);

        Ta stored = dao.findById("test@bupt.edu.cn").orElseThrow();
        assertEquals("test@bupt.edu.cn", stored.getEmail());
        assertFalse(stored.isDisabled());

        stored.setPhone("13800001111");
        dao.update(stored);

        Ta updated = dao.findById("test@bupt.edu.cn").orElseThrow();
        assertEquals("13800001111", updated.getPhone());
    }
}
