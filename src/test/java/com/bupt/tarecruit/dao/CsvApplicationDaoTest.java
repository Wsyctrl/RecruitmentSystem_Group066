package com.bupt.tarecruit.dao;

import com.bupt.tarecruit.entity.ApplicationRecord;
import com.bupt.tarecruit.entity.ApplicationStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CsvApplicationDao CSV persistence of applications.
 */
class CsvApplicationDaoTest {

    @TempDir
    Path tempDir;
    /** Verifies save update and query by ta and job. */
    @Test
    void saveUpdateAndQueryByTaAndJob() {
        CsvApplicationDao dao = new CsvApplicationDao(tempDir.resolve("Applications.csv"));
        ApplicationRecord record = new ApplicationRecord();
        record.setApplyId("apply001");
        record.setTaId("ta@bupt.edu.cn");
        record.setJobId("job001");
        record.setStatus(ApplicationStatus.PENDING);
        record.setApplyTime(LocalDateTime.of(2025, 1, 10, 9, 0));
        dao.save(record);

        assertEquals(record, dao.findById("apply001").orElseThrow());
        assertEquals(1, dao.findByTaId("ta@bupt.edu.cn").size());
        assertEquals(1, dao.findByJobId("job001").size());

        record.setStatus(ApplicationStatus.HIRED);
        record.setHiredTime(LocalDateTime.of(2025, 2, 1, 10, 0));
        dao.update(record);

        ApplicationRecord updated = dao.findById("apply001").orElseThrow();
        assertEquals(ApplicationStatus.HIRED, updated.getStatus());
        assertNotNull(updated.getHiredTime());

        List<ApplicationRecord> all = dao.findAll();
        assertEquals(1, all.size());
    }
    /** Verifies find by id case insensitive. */
    @Test
    void findByIdCaseInsensitive() {
        CsvApplicationDao dao = new CsvApplicationDao(tempDir.resolve("Applications.csv"));
        ApplicationRecord record = new ApplicationRecord();
        record.setApplyId("applyABC");
        record.setTaId("ta@bupt.edu.cn");
        record.setJobId("job001");
        record.setStatus(ApplicationStatus.PENDING);
        dao.save(record);

        assertTrue(dao.findById("applyabc").isPresent());
    }
}
