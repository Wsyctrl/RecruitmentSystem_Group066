package com.bupt.tarecruit.service;

import com.bupt.tarecruit.dao.CsvMoDao;
import com.bupt.tarecruit.dao.CsvTaDao;
import com.bupt.tarecruit.entity.Ta;
import com.bupt.tarecruit.util.FileStorageHelper;
import com.bupt.tarecruit.util.OperationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ProfileService profile updates and password changes.
 */
class ProfileServiceTest {

    @TempDir
    Path tempDir;

    private ProfileService profileService;
    private CsvTaDao taDao;
    private static final String TA_ID = "ta@bupt.edu.cn";

    @BeforeEach
    void setUp() {
        taDao = new CsvTaDao(tempDir.resolve("TA.csv"));
        profileService = new ProfileService(taDao, new CsvMoDao(tempDir.resolve("MO.csv")));
        Ta ta = new Ta(TA_ID, "Secret@1");
        ta.setEmail(TA_ID);
        ta.setMajor("Computer Science");
        ta.setCvPath(FileStorageHelper.cvRelativePath(TA_ID));
        ta.setAiSummary("Strong Java and teaching experience.");
        taDao.save(ta);
    }
    /** Verifies update ta clears ai summary when resume profile fields change. */
    @Test
    void updateTaClearsAiSummaryWhenResumeProfileFieldsChange() {
        Ta ta = taDao.findById(TA_ID).orElseThrow();
        ta.setMajor("Software Engineering");

        profileService.updateTa(ta);

        Ta stored = taDao.findById(TA_ID).orElseThrow();
        assertEquals("Software Engineering", stored.getMajor());
        assertTrue(stored.getAiSummary() == null || stored.getAiSummary().isBlank());
    }
    /** Verifies update ta preserves ai summary when only contact fields change. */
    @Test
    void updateTaPreservesAiSummaryWhenOnlyContactFieldsChange() {
        Ta ta = taDao.findById(TA_ID).orElseThrow();
        ta.setFullName("New Name");
        ta.setPhone("13800001111");
        ta.setPassword("Other@99");

        profileService.updateTa(ta);

        Ta stored = taDao.findById(TA_ID).orElseThrow();
        assertEquals("Strong Java and teaching experience.", stored.getAiSummary());
    }
    /** Verifies update ta clears ai summary when cv path removed. */
    @Test
    void updateTaClearsAiSummaryWhenCvPathRemoved() {
        Ta ta = taDao.findById(TA_ID).orElseThrow();
        ta.setCvPath("");
        profileService.updateTa(ta);

        Ta stored = taDao.findById(TA_ID).orElseThrow();
        assertTrue(stored.getCvPath() == null || stored.getCvPath().isBlank());
        assertTrue(stored.getAiSummary() == null || stored.getAiSummary().isBlank());
    }
    /** Verifies update ta clears ai summary when cv content changed flag is true. */
    @Test
    void updateTaClearsAiSummaryWhenCvContentChangedFlagIsTrue() {
        Ta ta = taDao.findById(TA_ID).orElseThrow();
        ta.setCvPath(FileStorageHelper.cvRelativePath(TA_ID));

        profileService.updateTa(ta, true);

        Ta stored = taDao.findById(TA_ID).orElseThrow();
        assertTrue(stored.getAiSummary() == null || stored.getAiSummary().isBlank());
    }
    /** Verifies update ta preserves ai summary when cv path unchanged and content unchanged. */
    @Test
    void updateTaPreservesAiSummaryWhenCvPathUnchangedAndContentUnchanged() {
        Ta ta = taDao.findById(TA_ID).orElseThrow();

        profileService.updateTa(ta, false);

        Ta stored = taDao.findById(TA_ID).orElseThrow();
        assertEquals("Strong Java and teaching experience.", stored.getAiSummary());
    }
    /** Verifies update ta preserves ai summary when only ai summary changes. */
    @Test
    void updateTaPreservesAiSummaryWhenOnlyAiSummaryChanges() {
        Ta ta = taDao.findById(TA_ID).orElseThrow();
        ta.setAiSummary("Refined one-line applicant summary.");

        profileService.updateTa(ta);

        Ta stored = taDao.findById(TA_ID).orElseThrow();
        assertEquals("Refined one-line applicant summary.", stored.getAiSummary());
        assertEquals("Computer Science", stored.getMajor());
    }
    /** Verifies should invalidate ai summary ignores ai summary column. */
    @Test
    void shouldInvalidateAiSummaryIgnoresAiSummaryColumn() {
        Ta stored = taDao.findById(TA_ID).orElseThrow();
        Ta updated = taDao.findById(TA_ID).orElseThrow();
        updated.setAiSummary("Different cached summary.");

        assertFalse(ProfileService.shouldInvalidateAiSummary(stored, updated, false));
    }
    /** Verifies change ta password success. */
    @Test
    void changeTaPasswordSuccess() {
        OperationResult<Void> result = profileService.changeTaPassword(
                TA_ID, "Secret@1", "NewSecret@9", "NewSecret@9");
        assertTrue(result.success());
        assertEquals("NewSecret@9", taDao.findById(TA_ID).orElseThrow().getPassword());
    }
    /** Verifies change ta password wrong current should fail. */
    @Test
    void changeTaPasswordWrongCurrentShouldFail() {
        OperationResult<Void> result = profileService.changeTaPassword(
                TA_ID, "Wrong@1", "NewSecret@9", "NewSecret@9");
        assertFalse(result.success());
        assertTrue(result.message().toLowerCase().contains("incorrect"));
    }
    /** Verifies change ta password mismatch confirm should fail. */
    @Test
    void changeTaPasswordMismatchConfirmShouldFail() {
        OperationResult<Void> result = profileService.changeTaPassword(
                TA_ID, "Secret@1", "NewSecret@9", "Other@9");
        assertFalse(result.success());
        assertTrue(result.message().toLowerCase().contains("match"));
    }
    /** Verifies change ta password blank new should fail. */
    @Test
    void changeTaPasswordBlankNewShouldFail() {
        OperationResult<Void> result = profileService.changeTaPassword(
                TA_ID, "Secret@1", "  ", "  ");
        assertFalse(result.success());
        assertTrue(result.message().toLowerCase().contains("required"));
    }
    /** Verifies update mo profile should persist. */
    @Test
    void updateMoProfileShouldPersist() {
        com.bupt.tarecruit.entity.Mo mo = new com.bupt.tarecruit.entity.Mo("mo@bupt.edu.cn", "Mo@1");
        mo.setEmail("mo@bupt.edu.cn");
        mo.setFullName("Original");
        new CsvMoDao(tempDir.resolve("MO.csv")).save(mo);

        ProfileService service = new ProfileService(taDao, new CsvMoDao(tempDir.resolve("MO.csv")));
        mo.setFullName("Updated MO");
        assertTrue(service.updateMo(mo).success());
        assertEquals("Updated MO", service.findMo("mo@bupt.edu.cn").orElseThrow().getFullName());
    }
    /** Verifies save cv detects content change. */
    @Test
    void saveCvDetectsContentChange() throws Exception {
        FileStorageHelper helper = new FileStorageHelper(tempDir);
        Path first = tempDir.resolve("cv-v1.txt");
        Path second = tempDir.resolve("cv-v2.txt");
        Files.writeString(first, "skills: Java");
        Files.writeString(second, "skills: Python");

        assertTrue(helper.saveCv(TA_ID, first.toFile()).contentChanged());
        assertFalse(helper.saveCv(TA_ID, first.toFile()).contentChanged());
        assertTrue(helper.saveCv(TA_ID, second.toFile()).contentChanged());
    }
}
