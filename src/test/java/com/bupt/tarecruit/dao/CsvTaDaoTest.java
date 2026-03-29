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

        Ta ta = new Ta("ta20239999", "Secret@1");
        ta.setEmail("test@bupt.edu.cn");
        dao.save(ta);

        Ta stored = dao.findById("ta20239999").orElseThrow();
        assertEquals("test@bupt.edu.cn", stored.getEmail());
        assertFalse(stored.isDisabled());

        stored.setPhone("13800001111");
        dao.update(stored);

        Ta updated = dao.findById("ta20239999").orElseThrow();
        assertEquals("13800001111", updated.getPhone());
    }
}
