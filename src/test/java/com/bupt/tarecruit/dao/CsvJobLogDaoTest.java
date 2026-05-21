package com.bupt.tarecruit.dao;

import com.bupt.tarecruit.entity.JobLog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CsvJobLogDao CSV persistence of job audit logs.
 */
class CsvJobLogDaoTest {

    @TempDir
    Path tempDir;
    /** Verifies save and load job log. */
    @Test
    void saveAndLoadJobLog() {
        CsvJobLogDao dao = new CsvJobLogDao(tempDir.resolve("JobLogs.csv"));
        JobLog log = new JobLog();
        log.setLogId("jlog001");
        log.setAdminId("admin@bupt.edu.cn");
        log.setJobId("job001");
        log.setAction(JobLog.JobLogAction.CLOSE_JOB);
        log.setPreviousState("Open");
        log.setNewState("Closed");
        log.setTimestamp(LocalDateTime.of(2025, 6, 1, 8, 30));
        dao.save(log);

        JobLog stored = dao.findAll().get(0);
        assertEquals("job001", stored.getJobId());
        assertEquals(JobLog.JobLogAction.CLOSE_JOB, stored.getAction());
    }
}
