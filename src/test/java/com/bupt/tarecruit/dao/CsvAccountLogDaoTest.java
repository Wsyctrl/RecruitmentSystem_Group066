package com.bupt.tarecruit.dao;

import com.bupt.tarecruit.entity.AccountLog;
import com.bupt.tarecruit.entity.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CsvAccountLogDao CSV persistence of account audit logs.
 */
class CsvAccountLogDaoTest {

    @TempDir
    Path tempDir;
    /** Verifies save and load account log. */
    @Test
    void saveAndLoadAccountLog() {
        CsvAccountLogDao dao = new CsvAccountLogDao(tempDir.resolve("AccountLogs.csv"));
        AccountLog log = new AccountLog();
        log.setLogId("alog001");
        log.setAdminId("admin@bupt.edu.cn");
        log.setTargetUserId("ta@bupt.edu.cn");
        log.setTargetRole(Role.TA);
        log.setAction(AccountLog.AccountAction.DISABLE);
        log.setPreviousState("Active");
        log.setNewState("Disabled");
        log.setTimestamp(LocalDateTime.of(2025, 5, 1, 12, 0));
        dao.save(log);

        AccountLog stored = dao.findAll().get(0);
        assertEquals("alog001", stored.getLogId());
        assertEquals(Role.TA, stored.getTargetRole());
        assertEquals(AccountLog.AccountAction.DISABLE, stored.getAction());
        assertEquals(LocalDateTime.of(2025, 5, 1, 12, 0), stored.getTimestamp());
    }
}
