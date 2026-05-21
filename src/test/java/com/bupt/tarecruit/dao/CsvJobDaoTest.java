package com.bupt.tarecruit.dao;

import com.bupt.tarecruit.entity.Job;
import com.bupt.tarecruit.entity.JobStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CsvJobDao CSV persistence of job postings.
 */
class CsvJobDaoTest {

    @TempDir
    Path tempDir;
    /** Verifies save update and round trip fields. */
    @Test
    void saveUpdateAndRoundTripFields() {
        CsvJobDao dao = new CsvJobDao(tempDir.resolve("Jobs.csv"));
        Job job = new Job();
        job.setJobId("job010");
        job.setJobName("Data Structures");
        job.setMoId("mo@bupt.edu.cn");
        job.setModuleName("CS");
        job.setNumberOfPositions(3);
        job.setRequirements("Java");
        job.setStartDate(LocalDate.of(2025, 9, 1));
        job.setEndDate(LocalDate.of(2025, 12, 31));
        job.setKeywords("java,ds");
        job.setStatus(JobStatus.OPEN);
        dao.save(job);

        Job stored = dao.findById("job010").orElseThrow();
        assertEquals("Data Structures", stored.getJobName());
        assertEquals(3, stored.getNumberOfPositions());
        assertEquals("java,ds", stored.getKeywords());
        assertTrue(stored.isOpen());

        stored.setStatus(JobStatus.CLOSED);
        stored.setKeywords("updated");
        dao.update(stored);

        Job updated = dao.findById("job010").orElseThrow();
        assertEquals(JobStatus.CLOSED, updated.getStatus());
        assertEquals("updated", updated.getKeywords());
    }
    /** Verifies invalid status code defaults to open. */
    @Test
    void invalidStatusCodeDefaultsToOpen() {
        CsvJobDao dao = new CsvJobDao(tempDir.resolve("Jobs.csv"));
        Job job = new Job();
        job.setJobId("job011");
        job.setJobName("Test");
        job.setMoId("mo@bupt.edu.cn");
        job.setModuleName("M");
        job.setNumberOfPositions(1);
        job.setStartDate(LocalDate.now());
        job.setEndDate(LocalDate.now().plusDays(30));
        job.setStatus(JobStatus.fromCode(99));
        dao.save(job);
        assertEquals(JobStatus.OPEN, dao.findById("job011").orElseThrow().getStatus());
    }
}
