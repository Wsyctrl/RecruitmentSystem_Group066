package com.bupt.tarecruit.service;

import com.bupt.tarecruit.dao.CsvApplicationDao;
import com.bupt.tarecruit.dao.CsvJobDao;
import com.bupt.tarecruit.entity.ApplicationRecord;
import com.bupt.tarecruit.entity.ApplicationStatus;
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
 * ApplicationService edge-case unit tests.
 *
 * This file focuses on business-rule boundaries that can easily regress:
 * 1) blocking duplicate active applications,
 * 2) enforcing ownership checks on withdraw,
 * 3) rejecting only pending records when a job is closed (hired records must remain unchanged).
 */
class ApplicationServiceEdgeCaseTest {

    @TempDir
    Path tempDir;
    /** Verifies duplicate active application should be rejected. */
    @Test
    void duplicateActiveApplicationShouldBeRejected() {
        CsvJobDao jobDao = new CsvJobDao(tempDir.resolve("Jobs.csv"));
        CsvApplicationDao applicationDao = new CsvApplicationDao(tempDir.resolve("Applications.csv"));
        ApplicationService service = new ApplicationService(applicationDao, jobDao);

        Job job = createOpenJob("job1001");
        jobDao.save(job);

        OperationResult<ApplicationRecord> first = service.applyForJob("ta20230001", job);
        OperationResult<ApplicationRecord> second = service.applyForJob("ta20230001", job);

        assertTrue(first.success());
        assertFalse(second.success());
        assertTrue(second.message().toLowerCase().contains("already"));
    }
    /** Verifies withdraw by different ta should fail. */
    @Test
    void withdrawByDifferentTaShouldFail() {
        CsvJobDao jobDao = new CsvJobDao(tempDir.resolve("Jobs.csv"));
        CsvApplicationDao applicationDao = new CsvApplicationDao(tempDir.resolve("Applications.csv"));
        ApplicationService service = new ApplicationService(applicationDao, jobDao);

        Job job = createOpenJob("job1002");
        jobDao.save(job);

        String applyId = service.applyForJob("ta20230002", job).data().getApplyId();
        OperationResult<Void> result = service.withdraw(applyId, "ta20239999");

        assertFalse(result.success());
        assertTrue(result.message().toLowerCase().contains("cannot"));
    }
    /** Verifies reject pending applications should only change pending records. */
    @Test
    void rejectPendingApplicationsShouldOnlyChangePendingRecords() {
        CsvJobDao jobDao = new CsvJobDao(tempDir.resolve("Jobs.csv"));
        CsvApplicationDao applicationDao = new CsvApplicationDao(tempDir.resolve("Applications.csv"));
        ApplicationService service = new ApplicationService(applicationDao, jobDao);

        Job job = createOpenJob("job1003");
        jobDao.save(job);

        String pendingId = service.applyForJob("ta20230003", job).data().getApplyId();
        String anotherId = service.applyForJob("ta20230004", job).data().getApplyId();

        // Turn one record into HIRED first, then close the job and reject remaining pending records.
        assertTrue(service.hireApplicant(anotherId).success());
        OperationResult<Void> result = service.rejectPendingApplicationsForJob(job.getJobId());

        assertTrue(result.success());

        List<ApplicationRecord> records = applicationDao.findByJobId(job.getJobId());
        ApplicationRecord pendingRecord = records.stream().filter(r -> r.getApplyId().equals(pendingId)).findFirst().orElseThrow();
        ApplicationRecord hiredRecord = records.stream().filter(r -> r.getApplyId().equals(anotherId)).findFirst().orElseThrow();

        assertEquals(ApplicationStatus.REJECTED, pendingRecord.getStatus());
        assertEquals(ApplicationStatus.HIRED, hiredRecord.getStatus());
    }
    /** Verifies withdraw unknown application should fail. */
    @Test
    void withdrawUnknownApplicationShouldFail() {
        CsvJobDao jobDao = new CsvJobDao(tempDir.resolve("Jobs.csv"));
        CsvApplicationDao applicationDao = new CsvApplicationDao(tempDir.resolve("Applications.csv"));
        ApplicationService service = new ApplicationService(applicationDao, jobDao);

        OperationResult<Void> result = service.withdraw("apply999", "ta20230001");
        assertFalse(result.success());
        assertTrue(result.message().contains("not found"));
    }
    /** Verifies hire unknown application should fail. */
    @Test
    void hireUnknownApplicationShouldFail() {
        CsvJobDao jobDao = new CsvJobDao(tempDir.resolve("Jobs.csv"));
        CsvApplicationDao applicationDao = new CsvApplicationDao(tempDir.resolve("Applications.csv"));
        ApplicationService service = new ApplicationService(applicationDao, jobDao);

        OperationResult<Void> result = service.hireApplicant("apply999");
        assertFalse(result.success());
        assertTrue(result.message().contains("not found"));
    }
    /** Verifies find active applications for ta excludes withdrawn. */
    @Test
    void findActiveApplicationsForTaExcludesWithdrawn() {
        CsvJobDao jobDao = new CsvJobDao(tempDir.resolve("Jobs.csv"));
        CsvApplicationDao applicationDao = new CsvApplicationDao(tempDir.resolve("Applications.csv"));
        ApplicationService service = new ApplicationService(applicationDao, jobDao);

        Job job = createOpenJob("job1004");
        jobDao.save(job);
        String applyId = service.applyForJob("ta20230005", job).data().getApplyId();
        service.withdraw(applyId, "ta20230005");

        assertTrue(service.findActiveApplicationsForTa("ta20230005").isEmpty());
        assertEquals(1, service.findByTa("ta20230005").size());
    }

    /**
     * @param jobId job identifier to assign
     * @return open job template for edge-case tests
     */
    private Job createOpenJob(String jobId) {
        Job job = new Job();
        job.setJobId(jobId);
        job.setJobName("Test Job " + jobId);
        job.setMoId("mo20160001");
        job.setModuleName("SE");
        job.setNumberOfPositions(1);
        job.setStartDate(LocalDate.now());
        job.setEndDate(LocalDate.now().plusMonths(2));
        job.setRequirements("Requirement");
        job.setStatus(JobStatus.OPEN);
        return job;
    }
}
