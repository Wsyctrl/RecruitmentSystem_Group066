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
 * 4) @bupt.edu.cn email validation during registration.
 * 管理员账号登录是否识别为 ADMIN
 * 禁用 TA 账号登录是否被拒绝
 * 重复注册是否被拒绝
 * 非法邮箱注册是否被拒绝
 */
class AuthServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void loginAdminMoShouldResolveToAdminRole() {
        CsvTaDao taDao = new CsvTaDao(tempDir.resolve("TA.csv"));
        CsvMoDao moDao = new CsvMoDao(tempDir.resolve("MO.csv"));
        AuthService service = new AuthService(taDao, moDao);

        Mo admin = new Mo("admin@bupt.edu.cn", "Admin@123");
        admin.setEmail("admin@bupt.edu.cn");
        admin.setDisabled(false);
        moDao.save(admin);

        OperationResult<UserSession> result = service.login("admin@bupt.edu.cn", "Admin@123");

        assertTrue(result.success());
        assertEquals(Role.ADMIN, result.data().role());
    }

    @Test
    void loginDisabledTaShouldFail() {
        CsvTaDao taDao = new CsvTaDao(tempDir.resolve("TA.csv"));
        CsvMoDao moDao = new CsvMoDao(tempDir.resolve("MO.csv"));
        AuthService service = new AuthService(taDao, moDao);

        Ta ta = new Ta("ta20230001@bupt.edu.cn", "Pass@123");
        ta.setEmail("ta20230001@bupt.edu.cn");
        ta.setDisabled(true);
        taDao.save(ta);

        OperationResult<UserSession> result = service.login("ta20230001@bupt.edu.cn", "Pass@123");

        assertFalse(result.success());
        assertTrue(result.message().toLowerCase().contains("disabled"));
    }

    @Test
    void registerDuplicateUserShouldFail() {
        CsvTaDao taDao = new CsvTaDao(tempDir.resolve("TA.csv"));
        CsvMoDao moDao = new CsvMoDao(tempDir.resolve("MO.csv"));
        AuthService service = new AuthService(taDao, moDao);

        Ta existing = new Ta("ta20235555@bupt.edu.cn", "OldPass@1");
        existing.setEmail("ta20235555@bupt.edu.cn");
        taDao.save(existing);

        OperationResult<Void> result = service.register(Role.TA, "ta20235555@bupt.edu.cn", "NewPass@1", "NewPass@1");

        assertFalse(result.success());
        assertTrue(result.message().toLowerCase().contains("exists"));
    }

    @Test
    void registerInvalidEmailShouldFail() {
        CsvTaDao taDao = new CsvTaDao(tempDir.resolve("TA.csv"));
        CsvMoDao moDao = new CsvMoDao(tempDir.resolve("MO.csv"));
        AuthService service = new AuthService(taDao, moDao);

        OperationResult<Void> result = service.register(Role.MO, "mo001", "Pass@123", "Pass@123");

        assertFalse(result.success());
        assertTrue(result.message().toLowerCase().contains("@bupt.edu.cn"));
    }
}
