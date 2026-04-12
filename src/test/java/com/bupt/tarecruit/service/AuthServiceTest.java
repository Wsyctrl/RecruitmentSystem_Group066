package com.bupt.tarecruit.service;

import com.bupt.tarecruit.dao.CsvMoDao;
import com.bupt.tarecruit.dao.CsvTaDao;
import com.bupt.tarecruit.entity.Mo;
import com.bupt.tarecruit.entity.Role;
import com.bupt.tarecruit.entity.Ta;
import com.bupt.tarecruit.entity.UserSession;
import com.bupt.tarecruit.util.OperationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AuthService unit tests.
 *
 * This file verifies login and registration behavior, including:
 * 1) role resolution (TA/MO/ADMIN),
 * 2) disabled-account rejection,
 * 3) duplicate registration checks,
 * 4) ID format validation during registration.
 * 管理员账号登录是否识别为 ADMIN
 * 禁用 TA 账号登录是否被拒绝
 * 重复注册是否被拒绝
 * 非法 MO ID 注册是否被拒绝
 */
class AuthServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void loginBlankUsernameShouldFail() {
        CsvTaDao taDao = new CsvTaDao(tempDir.resolve("TA.csv"));
        CsvMoDao moDao = new CsvMoDao(tempDir.resolve("MO.csv"));
        AuthService service = new AuthService(taDao, moDao);

        OperationResult<UserSession> blankId = service.login("  ", "any");
        assertFalse(blankId.success());

        OperationResult<UserSession> blankPass = service.login("user", "");
        assertFalse(blankPass.success());
    }

    @Test
    void loginAdminMoShouldResolveToAdminRole() {
        CsvTaDao taDao = new CsvTaDao(tempDir.resolve("TA.csv"));
        CsvMoDao moDao = new CsvMoDao(tempDir.resolve("MO.csv"));
        AuthService service = new AuthService(taDao, moDao);

        Mo admin = new Mo("admin", "Admin@123");
        admin.setDisabled(false);
        moDao.save(admin);

        OperationResult<UserSession> result = service.login("admin", "Admin@123");

        assertTrue(result.success());
        assertEquals(Role.ADMIN, result.data().role());
    }

    @Test
    void loginDisabledTaShouldFail() {
        CsvTaDao taDao = new CsvTaDao(tempDir.resolve("TA.csv"));
        CsvMoDao moDao = new CsvMoDao(tempDir.resolve("MO.csv"));
        AuthService service = new AuthService(taDao, moDao);

        Ta ta = new Ta("ta20230001", "Pass@123");
        ta.setDisabled(true);
        taDao.save(ta);

        OperationResult<UserSession> result = service.login("ta20230001", "Pass@123");

        assertFalse(result.success());
        assertTrue(result.message().toLowerCase().contains("disabled"));
    }

    @Test
    void registerDuplicateUserShouldFail() {
        CsvTaDao taDao = new CsvTaDao(tempDir.resolve("TA.csv"));
        CsvMoDao moDao = new CsvMoDao(tempDir.resolve("MO.csv"));
        AuthService service = new AuthService(taDao, moDao);

        taDao.save(new Ta("ta20235555", "OldPass@1"));

        OperationResult<Void> result = service.register(Role.TA, "ta20235555", "NewPass@1", "NewPass@1");

        assertFalse(result.success());
        assertTrue(result.message().toLowerCase().contains("exists"));
    }

    @Test
    void registerInvalidMoIdShouldFail() {
        CsvTaDao taDao = new CsvTaDao(tempDir.resolve("TA.csv"));
        CsvMoDao moDao = new CsvMoDao(tempDir.resolve("MO.csv"));
        AuthService service = new AuthService(taDao, moDao);

        OperationResult<Void> result = service.register(Role.MO, "mo001", "Pass@123", "Pass@123");

        assertFalse(result.success());
        assertTrue(result.message().toLowerCase().contains("invalid"));
    }
}
