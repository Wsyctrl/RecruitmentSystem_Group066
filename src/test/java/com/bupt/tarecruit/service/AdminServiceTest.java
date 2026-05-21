package com.bupt.tarecruit.service;

import com.bupt.tarecruit.dao.CsvAccountLogDao;
import com.bupt.tarecruit.dao.CsvJobDao;
import com.bupt.tarecruit.dao.CsvMoDao;
import com.bupt.tarecruit.dao.CsvTaDao;
import com.bupt.tarecruit.entity.AccountLog;
import com.bupt.tarecruit.entity.Job;
import com.bupt.tarecruit.entity.JobStatus;
import com.bupt.tarecruit.entity.Mo;
import com.bupt.tarecruit.entity.Role;
import com.bupt.tarecruit.entity.Ta;
import com.bupt.tarecruit.util.OperationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class AdminServiceTest {

    @TempDir
    Path tempDir;

    private AdminService adminService;
    private CsvTaDao taDao;
    private CsvMoDao moDao;
    private CsvJobDao jobDao;
    private CsvAccountLogDao accountLogDao;

    @BeforeEach
    void setUp() {
        taDao = new CsvTaDao(tempDir.resolve("TA.csv"));
        moDao = new CsvMoDao(tempDir.resolve("MO.csv"));
        jobDao = new CsvJobDao(tempDir.resolve("Jobs.csv"));
        accountLogDao = new CsvAccountLogDao(tempDir.resolve("AccountLogs.csv"));
        adminService = new AdminService(taDao, moDao, jobDao, accountLogDao);

        Ta ta = new Ta("ta@bupt.edu.cn", "Old@1");
        ta.setEmail("ta@bupt.edu.cn");
        taDao.save(ta);

        Mo mo = new Mo("mo@bupt.edu.cn", "Old@2");
        mo.setEmail("mo@bupt.edu.cn");
        moDao.save(mo);

        Job job = new Job();
        job.setJobId("job001");
        job.setJobName("Open Job");
        job.setMoId("mo@bupt.edu.cn");
        job.setModuleName("CS");
        job.setNumberOfPositions(1);
        job.setStartDate(LocalDate.now());
        job.setEndDate(LocalDate.now().plusMonths(1));
        job.setStatus(JobStatus.OPEN);
        jobDao.save(job);
    }

    @Test
    void resetPasswordForTaShouldUseDefaultAndLog() {
        OperationResult<Void> result = adminService.resetPassword(Role.TA, "ta@bupt.edu.cn", "admin@bupt.edu.cn");

        assertTrue(result.success());
        assertEquals("Pass@123", taDao.findById("ta@bupt.edu.cn").orElseThrow().getPassword());
        assertEquals(1, accountLogDao.findAll().size());
        assertEquals(AccountLog.AccountAction.RESET_PASSWORD, accountLogDao.findAll().get(0).getAction());
    }

    @Test
    void resetPasswordUnknownTaShouldFail() {
        OperationResult<Void> result = adminService.resetPassword(Role.TA, "missing@bupt.edu.cn", "admin@bupt.edu.cn");
        assertFalse(result.success());
        assertTrue(result.message().contains("not found"));
    }

    @Test
    void toggleStatusDisableAndEnableTa() {
        assertTrue(adminService.toggleStatus(Role.TA, "ta@bupt.edu.cn", true, "admin@bupt.edu.cn").success());
        assertTrue(taDao.findById("ta@bupt.edu.cn").orElseThrow().isDisabled());

        assertTrue(adminService.toggleStatus(Role.TA, "ta@bupt.edu.cn", false, "admin@bupt.edu.cn").success());
        assertFalse(taDao.findById("ta@bupt.edu.cn").orElseThrow().isDisabled());
    }

    @Test
    void toggleJobOpenClosedShouldCloseOpenJob() {
        OperationResult<Void> result = adminService.toggleJobOpenClosed("job001");
        assertTrue(result.success());
        assertEquals(JobStatus.CLOSED, jobDao.findById("job001").orElseThrow().getStatus());
    }

    @Test
    void toggleJobOpenClosedAlreadyClosedShouldFail() {
        adminService.toggleJobOpenClosed("job001");
        OperationResult<Void> second = adminService.toggleJobOpenClosed("job001");
        assertFalse(second.success());
        assertTrue(second.message().toLowerCase().contains("closed"));
    }

    @Test
    void toggleJobOpenClosedUnknownJobShouldFail() {
        OperationResult<Void> result = adminService.toggleJobOpenClosed("job999");
        assertFalse(result.success());
        assertTrue(result.message().contains("not found"));
    }

    @Test
    void findAllMethodsReturnPersistedData() {
        assertEquals(1, adminService.findAllTa().size());
        assertEquals(1, adminService.findAllMo().size());
        assertEquals(1, adminService.findAllJobs().size());
        assertTrue(adminService.findAllAccountLogs().isEmpty());
    }
}
