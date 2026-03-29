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

class ApplicationServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void applyAndHireApplicantFlow() {
        Path jobFile = tempDir.resolve("Jobs.csv");
        Path appFile = tempDir.resolve("Applications.csv");
        CsvJobDao jobDao = new CsvJobDao(jobFile);
        CsvApplicationDao applicationDao = new CsvApplicationDao(appFile);
        ApplicationService service = new ApplicationService(applicationDao, jobDao);

        Job job = new Job();
        job.setJobId("job900");
        job.setJobName("Test job");
        job.setMoId("mo20160001");
        job.setModuleName("Algorithms");
        job.setNumberOfPositions(1);
        job.setStartDate(LocalDate.now());
        job.setEndDate(LocalDate.now().plusMonths(3));
        job.setStatus(JobStatus.OPEN);
        jobDao.save(job);

        OperationResult<ApplicationRecord> result1 = service.applyForJob("ta20230101", job);
        assertTrue(result1.success());
        String hireTargetId = result1.data().getApplyId();

        OperationResult<ApplicationRecord> result2 = service.applyForJob("ta20230202", job);
        assertTrue(result2.success());

        OperationResult<Void> hireResult = service.hireApplicant(hireTargetId);
        assertTrue(hireResult.success());

        Job updatedJob = jobDao.findById("job900").orElseThrow();
        assertEquals(JobStatus.CLOSED, updatedJob.getStatus());

        List<ApplicationRecord> records = applicationDao.findByJobId("job900");
        assertEquals(2, records.size());

        ApplicationRecord hired = records.stream()
                .filter(r -> r.getApplyId().equals(hireTargetId))
                .findFirst()
                .orElseThrow();
        assertEquals(ApplicationStatus.HIRED, hired.getStatus());

        ApplicationRecord rejected = records.stream()
                .filter(r -> !r.getApplyId().equals(hireTargetId))
                .findFirst()
                .orElseThrow();
        assertEquals(ApplicationStatus.REJECTED, rejected.getStatus());
    }

    @Test
    void withdrawThenCanApplyAgain() {
        Path jobFile = tempDir.resolve("Jobs.csv");
        Path appFile = tempDir.resolve("Applications.csv");
        CsvJobDao jobDao = new CsvJobDao(jobFile);
        CsvApplicationDao applicationDao = new CsvApplicationDao(appFile);
        ApplicationService service = new ApplicationService(applicationDao, jobDao);

        Job job = new Job();
        job.setJobId("job901");
        job.setJobName("Re-apply test");
        job.setMoId("mo20160001");
        job.setModuleName("Test");
        job.setNumberOfPositions(2);
        job.setStartDate(LocalDate.now());
        job.setEndDate(LocalDate.now().plusMonths(1));
        job.setStatus(JobStatus.OPEN);
        jobDao.save(job);

        OperationResult<ApplicationRecord> first = service.applyForJob("ta20230303", job);
        assertTrue(first.success());
        String applyId = first.data().getApplyId();

        assertTrue(service.withdraw(applyId, "ta20230303").success());

        OperationResult<ApplicationRecord> second = service.applyForJob("ta20230303", job);
        assertTrue(second.success(), "Should allow applying again after withdraw");
    }
}
