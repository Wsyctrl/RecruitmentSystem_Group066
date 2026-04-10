package com.bupt.tarecruit.service;

import com.bupt.tarecruit.dao.CsvJobDao;
import com.bupt.tarecruit.entity.Job;
import com.bupt.tarecruit.entity.JobStatus;
import com.bupt.tarecruit.util.OperationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JobService unit tests.
 *
 * This file verifies job lifecycle logic, including:
 * 1) job creation defaults,
 * 2) validation rules for invalid date ranges,
 * 3) open-job filtering and keyword search behavior.
 * 新建岗位是否自动生成 ID 并默认为 OPEN
 * 结束日期早于开始日期是否抛错
 * 搜索开放岗位时是否排除 CLOSED 岗位
 */
class JobServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void upsertNewJobShouldGenerateIdAndSetOpenStatus() {
        CsvJobDao jobDao = new CsvJobDao(tempDir.resolve("Jobs.csv"));
        JobService service = new JobService(jobDao);

        Job job = createValidJob("Linear Algebra Tutor", "Math", LocalDate.now().plusDays(1));

        OperationResult<Job> result = service.upsertJob(job);

        assertTrue(result.success());
        assertNotNull(result.data().getJobId());
        assertFalse(result.data().getJobId().isBlank());
        assertEquals(JobStatus.OPEN, result.data().getStatus());
    }

    @Test
    void upsertJobWithEndDateBeforeStartDateShouldThrow() {
        CsvJobDao jobDao = new CsvJobDao(tempDir.resolve("Jobs.csv"));
        JobService service = new JobService(jobDao);

        Job job = createValidJob("Invalid Date Range", "Physics", LocalDate.now().plusDays(5));
        job.setEndDate(job.getStartDate().minusDays(1));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.upsertJob(job));
        assertTrue(ex.getMessage().toLowerCase().contains("end date"));
    }

    @Test
    void searchOpenJobsShouldIgnoreClosedJobsAndMatchKeyword() {
        CsvJobDao jobDao = new CsvJobDao(tempDir.resolve("Jobs.csv"));
        JobService service = new JobService(jobDao);

        Job openMatched = createValidJob("Algorithms Tutor", "CS", LocalDate.now().plusDays(2));
        openMatched.setRequirements("Need strong Java and data structure knowledge");
        service.upsertJob(openMatched);

        Job closedMatched = createValidJob("Algorithms Marker", "CS", LocalDate.now().plusDays(3));
        closedMatched.setRequirements("Java knowledge required");
        closedMatched.setStatus(JobStatus.CLOSED);
        jobDao.save(closedMatched);

        List<Job> results = service.searchOpenJobs("java");

        assertEquals(1, results.size());
        assertEquals("Algorithms Tutor", results.get(0).getJobName());
        assertTrue(results.get(0).isOpen());
    }

    private Job createValidJob(String jobName, String module, LocalDate startDate) {
        Job job = new Job();
        job.setJobName(jobName);
        job.setModuleName(module);
        job.setMoId("mo20160001");
        job.setNumberOfPositions(2);
        job.setStartDate(startDate);
        job.setEndDate(startDate.plusMonths(1));
        job.setRequirements("Basic requirement");
        job.setAdditionalNotes("N/A");
        return job;
    }
}
