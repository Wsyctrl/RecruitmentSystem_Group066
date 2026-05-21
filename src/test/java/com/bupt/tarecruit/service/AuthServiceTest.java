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
 * 4) {@code @bupt.edu.cn} email validation during registration.
 */
class AuthServiceTest {

    @TempDir
    Path tempDir;
    /** Verifies login admin mo should resolve to admin role. */
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
    /** Verifies login disabled ta should fail. */
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
    /** Verifies register duplicate user should fail. */
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
    /** Verifies register invalid email should fail. */
    @Test
    void registerInvalidEmailShouldFail() {
        CsvTaDao taDao = new CsvTaDao(tempDir.resolve("TA.csv"));
        CsvMoDao moDao = new CsvMoDao(tempDir.resolve("MO.csv"));
        AuthService service = new AuthService(taDao, moDao);

        OperationResult<Void> result = service.register(Role.MO, "mo001", "Pass@123", "Pass@123");

        assertFalse(result.success());
        assertTrue(result.message().toLowerCase().contains("@bupt.edu.cn"));
    }
    /** Verifies login ta success should return ta session. */
    @Test
    void loginTaSuccessShouldReturnTaSession() {
        CsvTaDao taDao = new CsvTaDao(tempDir.resolve("TA.csv"));
        CsvMoDao moDao = new CsvMoDao(tempDir.resolve("MO.csv"));
        AuthService service = new AuthService(taDao, moDao);

        Ta ta = new Ta("ta20230001@bupt.edu.cn", "Pass@123");
        ta.setEmail("ta20230001@bupt.edu.cn");
        taDao.save(ta);

        OperationResult<UserSession> result = service.login("ta20230001@bupt.edu.cn", "Pass@123");

        assertTrue(result.success());
        assertEquals(Role.TA, result.data().role());
        assertTrue(result.data().taOptional().isPresent());
    }
    /** Verifies login mo success should return mo session. */
    @Test
    void loginMoSuccessShouldReturnMoSession() {
        CsvTaDao taDao = new CsvTaDao(tempDir.resolve("TA.csv"));
        CsvMoDao moDao = new CsvMoDao(tempDir.resolve("MO.csv"));
        AuthService service = new AuthService(taDao, moDao);

        Mo mo = new Mo("mo20160001@bupt.edu.cn", "MoPass@1");
        mo.setEmail("mo20160001@bupt.edu.cn");
        moDao.save(mo);

        OperationResult<UserSession> result = service.login("mo20160001@bupt.edu.cn", "MoPass@1");

        assertTrue(result.success());
        assertEquals(Role.MO, result.data().role());
        assertTrue(result.data().moOptional().isPresent());
    }
    /** Verifies login wrong password should fail. */
    @Test
    void loginWrongPasswordShouldFail() {
        CsvTaDao taDao = new CsvTaDao(tempDir.resolve("TA.csv"));
        CsvMoDao moDao = new CsvMoDao(tempDir.resolve("MO.csv"));
        AuthService service = new AuthService(taDao, moDao);

        Ta ta = new Ta("ta20230002@bupt.edu.cn", "Correct@1");
        ta.setEmail("ta20230002@bupt.edu.cn");
        taDao.save(ta);

        OperationResult<UserSession> result = service.login("ta20230002@bupt.edu.cn", "Wrong@1");

        assertFalse(result.success());
        assertTrue(result.message().toLowerCase().contains("password"));
    }
    /** Verifies login unknown user should fail. */
    @Test
    void loginUnknownUserShouldFail() {
        CsvTaDao taDao = new CsvTaDao(tempDir.resolve("TA.csv"));
        CsvMoDao moDao = new CsvMoDao(tempDir.resolve("MO.csv"));
        AuthService service = new AuthService(taDao, moDao);

        OperationResult<UserSession> result = service.login("unknown@bupt.edu.cn", "Pass@123");

        assertFalse(result.success());
        assertTrue(result.message().toLowerCase().contains("unknown"));
    }
    /** Verifies login blank fields should throw. */
    @Test
    void loginBlankFieldsShouldThrow() {
        CsvTaDao taDao = new CsvTaDao(tempDir.resolve("TA.csv"));
        CsvMoDao moDao = new CsvMoDao(tempDir.resolve("MO.csv"));
        AuthService service = new AuthService(taDao, moDao);

        assertThrows(IllegalArgumentException.class, () -> service.login("", "Pass@123"));
        assertThrows(IllegalArgumentException.class, () -> service.login("ta@bupt.edu.cn", "  "));
    }
    /** Verifies login invalid email format should fail. */
    @Test
    void loginInvalidEmailFormatShouldFail() {
        CsvTaDao taDao = new CsvTaDao(tempDir.resolve("TA.csv"));
        CsvMoDao moDao = new CsvMoDao(tempDir.resolve("MO.csv"));
        AuthService service = new AuthService(taDao, moDao);

        OperationResult<UserSession> result = service.login("not-an-email", "Pass@123");

        assertFalse(result.success());
        assertTrue(result.message().contains("@bupt.edu.cn"));
    }
    /** Verifies register ta success should persist. */
    @Test
    void registerTaSuccessShouldPersist() {
        CsvTaDao taDao = new CsvTaDao(tempDir.resolve("TA.csv"));
        CsvMoDao moDao = new CsvMoDao(tempDir.resolve("MO.csv"));
        AuthService service = new AuthService(taDao, moDao);

        OperationResult<Void> result = service.register(Role.TA, "newta@bupt.edu.cn", "Pass@123", "Pass@123");

        assertTrue(result.success());
        assertTrue(taDao.findById("newta@bupt.edu.cn").isPresent());
        assertFalse(taDao.findById("newta@bupt.edu.cn").orElseThrow().isDisabled());
    }
    /** Verifies register password mismatch should fail. */
    @Test
    void registerPasswordMismatchShouldFail() {
        CsvTaDao taDao = new CsvTaDao(tempDir.resolve("TA.csv"));
        CsvMoDao moDao = new CsvMoDao(tempDir.resolve("MO.csv"));
        AuthService service = new AuthService(taDao, moDao);

        OperationResult<Void> result = service.register(Role.TA, "newmo@bupt.edu.cn", "Pass@123", "Pass@999");

        assertFalse(result.success());
        assertTrue(result.message().toLowerCase().contains("match"));
    }
    /** Verifies register admin role should throw. */
    @Test
    void registerAdminRoleShouldThrow() {
        CsvTaDao taDao = new CsvTaDao(tempDir.resolve("TA.csv"));
        CsvMoDao moDao = new CsvMoDao(tempDir.resolve("MO.csv"));
        AuthService service = new AuthService(taDao, moDao);

        assertThrows(IllegalStateException.class,
                () -> service.register(Role.ADMIN, "admin2@bupt.edu.cn", "Pass@123", "Pass@123"));
    }
    /** Verifies login disabled mo should fail. */
    @Test
    void loginDisabledMoShouldFail() {
        CsvTaDao taDao = new CsvTaDao(tempDir.resolve("TA.csv"));
        CsvMoDao moDao = new CsvMoDao(tempDir.resolve("MO.csv"));
        AuthService service = new AuthService(taDao, moDao);

        Mo mo = new Mo("mo20160002@bupt.edu.cn", "Pass@123");
        mo.setEmail("mo20160002@bupt.edu.cn");
        mo.setDisabled(true);
        moDao.save(mo);

        OperationResult<UserSession> result = service.login("mo20160002@bupt.edu.cn", "Pass@123");

        assertFalse(result.success());
        assertTrue(result.message().toLowerCase().contains("disabled"));
    }
}
