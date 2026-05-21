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

    @Test
    void closeAndOpenJobLifecycle() {
        CsvJobDao jobDao = new CsvJobDao(tempDir.resolve("Jobs.csv"));
        JobService service = new JobService(jobDao);

        Job job = createValidJob("Lifecycle Job", "Math", LocalDate.now().plusDays(1));
        String jobId = service.upsertJob(job).data().getJobId();

        assertTrue(service.closeJob(jobId).success());
        assertFalse(jobDao.findById(jobId).orElseThrow().isOpen());

        assertTrue(service.openJob(jobId).success());
        assertTrue(jobDao.findById(jobId).orElseThrow().isOpen());
    }

    @Test
    void closeJobNotFoundShouldFail() {
        CsvJobDao jobDao = new CsvJobDao(tempDir.resolve("Jobs.csv"));
        JobService service = new JobService(jobDao);

        OperationResult<Void> result = service.closeJob("job999");
        assertFalse(result.success());
        assertTrue(result.message().contains("not found"));
    }

    @Test
    void upsertExistingJobShouldUpdateWithoutNewId() {
        CsvJobDao jobDao = new CsvJobDao(tempDir.resolve("Jobs.csv"));
        JobService service = new JobService(jobDao);

        Job job = createValidJob("Original", "CS", LocalDate.now().plusDays(2));
        String jobId = service.upsertJob(job).data().getJobId();

        Job toUpdate = jobDao.findById(jobId).orElseThrow();
        toUpdate.setJobName("Updated Title");
        OperationResult<Job> result = service.upsertJob(toUpdate);

        assertTrue(result.success());
        assertEquals("Updated Title", jobDao.findById(jobId).orElseThrow().getJobName());
    }

    @Test
    void upsertMissingRequiredFieldsShouldThrow() {
        CsvJobDao jobDao = new CsvJobDao(tempDir.resolve("Jobs.csv"));
        JobService service = new JobService(jobDao);

        Job incomplete = new Job();
        incomplete.setMoId("mo@bupt.edu.cn");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.upsertJob(incomplete));
        assertTrue(ex.getMessage().toLowerCase().contains("required"));
    }

    @Test
    void findJobsByMoFiltersAndSorts() {
        CsvJobDao jobDao = new CsvJobDao(tempDir.resolve("Jobs.csv"));
        JobService service = new JobService(jobDao);

        Job a = createValidJob("A", "CS", LocalDate.now());
        a.setMoId("mo-a@bupt.edu.cn");
        Job b = createValidJob("B", "CS", LocalDate.now());
        b.setMoId("mo-b@bupt.edu.cn");
        service.upsertJob(a);
        service.upsertJob(b);

        assertEquals(1, service.findJobsByMo("mo-a@bupt.edu.cn").size());
    }

    @Test
    void searchOpenJobsNullKeywordReturnsAllOpen() {
        CsvJobDao jobDao = new CsvJobDao(tempDir.resolve("Jobs.csv"));
        JobService service = new JobService(jobDao);

        service.upsertJob(createValidJob("Job One", "CS", LocalDate.now()));
        Job closed = createValidJob("Job Two", "CS", LocalDate.now());
        closed.setStatus(JobStatus.CLOSED);
        jobDao.save(closed);

        assertEquals(1, service.searchOpenJobs(null).size());
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
