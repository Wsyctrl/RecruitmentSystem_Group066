package com.bupt.tarecruit.dao;

import com.bupt.tarecruit.entity.Mo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvMoDaoTest {

    @TempDir
    Path tempDir;

    @Test
    void saveFindUpdateAndFindAll() {
        CsvMoDao dao = new CsvMoDao(tempDir.resolve("MO.csv"));
        Mo mo = new Mo("mo@bupt.edu.cn", "Secret@1");
        mo.setEmail("mo@bupt.edu.cn");
        mo.setFullName("Dr. Evans");
        mo.setResponsibleModules("Algorithms");
        mo.setPhone("01012345678");
        dao.save(mo);

        Mo stored = dao.findById("mo@bupt.edu.cn").orElseThrow();
        assertEquals("Dr. Evans", stored.getFullName());
        assertEquals("Algorithms", stored.getResponsibleModules());

        stored.setPhone("01087654321");
        stored.setDisabled(true);
        dao.update(stored);

        Mo updated = dao.findById("MO@bupt.edu.cn").orElseThrow();
        assertEquals("01087654321", updated.getPhone());
        assertTrue(updated.isDisabled());

        List<Mo> all = dao.findAll();
        assertEquals(1, all.size());
    }

    @Test
    void findByIdMissingReturnsEmpty() {
        CsvMoDao dao = new CsvMoDao(tempDir.resolve("MO.csv"));
        assertTrue(dao.findById("none@bupt.edu.cn").isEmpty());
    }
}
