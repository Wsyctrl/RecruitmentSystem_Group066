package com.bupt.tarecruit.service;

import com.bupt.tarecruit.dao.CsvApplicationDao;
import com.bupt.tarecruit.dao.CsvJobDao;
import com.bupt.tarecruit.entity.ApplicationRecord;
import com.bupt.tarecruit.entity.ApplicationStatus;
import com.bupt.tarecruit.entity.Job;
import com.bupt.tarecruit.entity.JobStatus;
import com.bupt.tarecruit.util.OperationResult;
import com.bupt.tarecruit.util.WorkloadRules;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ApplicationService state-transition and workload boundary tests.
 */
class ApplicationServiceStateTest {

    @TempDir
    Path tempDir;
    /** Verifies apply to closed job should fail. */
    @Test
    void applyToClosedJobShouldFail() {
        ApplicationService service = createService();
        Job job = createOpenJob("job2001");
        job.setStatus(JobStatus.CLOSED);
        jobDao(service).save(job);

        OperationResult<ApplicationRecord> result = service.applyForJob("ta@bupt.edu.cn", job);
        assertFalse(result.success());
        assertTrue(result.message().toLowerCase().contains("not open"));
    }
    /** Verifies withdraw non pending should fail. */
    @Test
    void withdrawNonPendingShouldFail() {
        ApplicationService service = createService();
        Job job = createOpenJob("job2002");
        jobDao(service).save(job);

        String applyId = service.applyForJob("ta@bupt.edu.cn", job).data().getApplyId();
        assertTrue(service.hireApplicant(applyId).success());

        OperationResult<Void> withdraw = service.withdraw(applyId, "ta@bupt.edu.cn");
        assertFalse(withdraw.success());
        assertTrue(withdraw.message().toLowerCase().contains("pending"));
    }
    /** Verifies unhire reject and unreject flow. */
    @Test
    void unhireRejectAndUnrejectFlow() {
        ApplicationService service = createService();
        Job job = createOpenJob("job2003");
        job.setNumberOfPositions(5);
        jobDao(service).save(job);

        String applyId = service.applyForJob("ta@bupt.edu.cn", job).data().getApplyId();
        assertTrue(service.hireApplicant(applyId).success());
        assertTrue(service.unhireApplicant(applyId).success());
        assertEquals(ApplicationStatus.PENDING, appDao(service).findById(applyId).orElseThrow().getStatus());

        assertTrue(service.rejectApplicant(applyId).success());
        assertEquals(ApplicationStatus.REJECTED, appDao(service).findById(applyId).orElseThrow().getStatus());

        assertTrue(service.unrejectApplicant(applyId).success());
        assertEquals(ApplicationStatus.PENDING, appDao(service).findById(applyId).orElseThrow().getStatus());
    }
    /** Verifies unhire non hired should fail. */
    @Test
    void unhireNonHiredShouldFail() {
        ApplicationService service = createService();
        Job job = createOpenJob("job2004");
        jobDao(service).save(job);
        String applyId = service.applyForJob("ta@bupt.edu.cn", job).data().getApplyId();

        OperationResult<Void> result = service.unhireApplicant(applyId);
        assertFalse(result.success());
        assertTrue(result.message().toLowerCase().contains("hired"));
    }
    /** Verifies hire multiple positions before closing. */
    @Test
    void hireMultiplePositionsBeforeClosing() {
        ApplicationService service = createService();
        Job job = createOpenJob("job2005");
        job.setNumberOfPositions(2);
        jobDao(service).save(job);

        String id1 = service.applyForJob("ta1@bupt.edu.cn", job).data().getApplyId();
        String id2 = service.applyForJob("ta2@bupt.edu.cn", job).data().getApplyId();
        String id3 = service.applyForJob("ta3@bupt.edu.cn", job).data().getApplyId();

        assertTrue(service.hireApplicant(id1).success());
        assertTrue(jobDao(service).findById(job.getJobId()).orElseThrow().isOpen());

        assertTrue(service.hireApplicant(id2).success());
        assertEquals(JobStatus.CLOSED, jobDao(service).findById(job.getJobId()).orElseThrow().getStatus());
        assertEquals(ApplicationStatus.REJECTED, appDao(service).findById(id3).orElseThrow().getStatus());
    }
    /** Verifies find active applications for job dedupes same ta. */
    @Test
    void findActiveApplicationsForJobDedupesSameTa() {
        ApplicationService service = createService();
        Job job = createOpenJob("job2006");
        jobDao(service).save(job);

        ApplicationRecord older = new ApplicationRecord();
        older.setApplyId("apply-old");
        older.setTaId("ta@bupt.edu.cn");
        older.setJobId(job.getJobId());
        older.setStatus(ApplicationStatus.PENDING);
        appDao(service).save(older);

        ApplicationRecord newer = new ApplicationRecord();
        newer.setApplyId("apply-new");
        newer.setTaId("ta@bupt.edu.cn");
        newer.setJobId(job.getJobId());
        newer.setStatus(ApplicationStatus.PENDING);
        appDao(service).save(newer);

        List<ApplicationRecord> active = service.findActiveApplicationsForJob(job.getJobId());
        assertEquals(1, active.size());
    }
    /** Verifies overlapping hired jobs triggers concurrent warning. */
    @Test
    void overlappingHiredJobsTriggersConcurrentWarning() {
        ApplicationService service = createService();
        LocalDate base = LocalDate.of(2025, 6, 1);

        Job hired1 = createOpenJob("job-h1");
        hired1.setStartDate(base);
        hired1.setEndDate(base.plusMonths(2));
        jobDao(service).save(hired1);

        Job hired2 = createOpenJob("job-h2");
        hired2.setStartDate(base.plusMonths(1));
        hired2.setEndDate(base.plusMonths(3));
        jobDao(service).save(hired2);

        Job target = createOpenJob("job-target");
        target.setStartDate(base.plusWeeks(2));
        target.setEndDate(base.plusMonths(4));
        jobDao(service).save(target);

        String taId = "ta@bupt.edu.cn";
        hire(service, taId, hired1);
        hire(service, taId, hired2);

        List<Job> overlaps = service.findOverlappingHiredJobs(taId, target.getJobId());
        assertEquals(2, overlaps.size());
        assertTrue(service.shouldWarnConcurrentHire(taId, target.getJobId()));
        assertEquals(WorkloadRules.CONCURRENT_JOB_WARNING_THRESHOLD, overlaps.size());
    }
    /** Verifies normalize pending applications for closed jobs on startup. */
    @Test
    void normalizePendingApplicationsForClosedJobsOnStartup() {
        CsvJobDao jobDao = new CsvJobDao(tempDir.resolve("Jobs.csv"));
        CsvApplicationDao appDao = new CsvApplicationDao(tempDir.resolve("Applications.csv"));
        ApplicationService service = new ApplicationService(appDao, jobDao);

        Job closed = createOpenJob("job-closed");
        closed.setStatus(JobStatus.CLOSED);
        jobDao.save(closed);

        ApplicationRecord pending = new ApplicationRecord();
        pending.setApplyId("apply-p");
        pending.setTaId("ta@bupt.edu.cn");
        pending.setJobId(closed.getJobId());
        pending.setStatus(ApplicationStatus.PENDING);
        appDao.save(pending);

        service.normalizePendingApplicationsForClosedJobs();

        assertEquals(ApplicationStatus.REJECTED, appDao.findById("apply-p").orElseThrow().getStatus());
    }
    /** Verifies find current ongoing hired jobs uses date window. */
    @Test
    void findCurrentOngoingHiredJobsUsesDateWindow() {
        ApplicationService service = createService();
        LocalDate today = LocalDate.of(2025, 7, 15);

        Job ongoing = createOpenJob("job-ongoing");
        ongoing.setStartDate(today.minusDays(5));
        ongoing.setEndDate(today.plusDays(10));
        jobDao(service).save(ongoing);

        Job future = createOpenJob("job-future");
        future.setStartDate(today.plusMonths(1));
        future.setEndDate(today.plusMonths(3));
        jobDao(service).save(future);

        String taId = "ta@bupt.edu.cn";
        hire(service, taId, ongoing);
        hire(service, taId, future);

        assertEquals(1, service.countCurrentOngoingHiredJobs(taId, today));
        assertEquals(1, service.findCurrentOngoingHiredJobs(taId, today).size());
    }

    private void hire(ApplicationService service, String taId, Job job) {
        service.applyForJob(taId, job);
        String applyId = appDao(service).findByTaId(taId).stream()
                .filter(r -> r.getJobId().equalsIgnoreCase(job.getJobId()))
                .findFirst()
                .orElseThrow()
                .getApplyId();
        service.hireApplicant(applyId);
    }

    private ApplicationService createService() {
        return new ApplicationService(
                new CsvApplicationDao(tempDir.resolve("Applications.csv")),
                new CsvJobDao(tempDir.resolve("Jobs.csv")));
    }

    private CsvJobDao jobDao(ApplicationService ignored) {
        return new CsvJobDao(tempDir.resolve("Jobs.csv"));
    }

    private CsvApplicationDao appDao(ApplicationService ignored) {
        return new CsvApplicationDao(tempDir.resolve("Applications.csv"));
    }

    private Job createOpenJob(String jobId) {
        Job job = new Job();
        job.setJobId(jobId);
        job.setJobName("Job " + jobId);
        job.setMoId("mo@bupt.edu.cn");
        job.setModuleName("SE");
        job.setNumberOfPositions(1);
        job.setStartDate(LocalDate.now());
        job.setEndDate(LocalDate.now().plusMonths(2));
        job.setRequirements("Req");
        job.setStatus(JobStatus.OPEN);
        return job;
    }
}
