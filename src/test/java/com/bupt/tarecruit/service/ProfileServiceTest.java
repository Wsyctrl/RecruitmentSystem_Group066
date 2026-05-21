package com.bupt.tarecruit.service;

import com.bupt.tarecruit.dao.CsvMoDao;
import com.bupt.tarecruit.dao.CsvTaDao;
import com.bupt.tarecruit.entity.Ta;
import com.bupt.tarecruit.util.FileStorageHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    void updateTaClearsAiSummaryWhenResumeProfileFieldsChange() {
        Ta ta = taDao.findById(TA_ID).orElseThrow();
        ta.setMajor("Software Engineering");

        profileService.updateTa(ta);

        Ta stored = taDao.findById(TA_ID).orElseThrow();
        assertEquals("Software Engineering", stored.getMajor());
        assertTrue(stored.getAiSummary() == null || stored.getAiSummary().isBlank());
    }

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

    @Test
    void updateTaClearsAiSummaryWhenCvPathRemoved() {
        Ta ta = taDao.findById(TA_ID).orElseThrow();
        ta.setCvPath("");
        profileService.updateTa(ta);

        Ta stored = taDao.findById(TA_ID).orElseThrow();
        assertTrue(stored.getCvPath() == null || stored.getCvPath().isBlank());
        assertTrue(stored.getAiSummary() == null || stored.getAiSummary().isBlank());
    }

    @Test
    void updateTaClearsAiSummaryWhenCvContentChangedFlagIsTrue() {
        Ta ta = taDao.findById(TA_ID).orElseThrow();
        ta.setCvPath(FileStorageHelper.cvRelativePath(TA_ID));

        profileService.updateTa(ta, true);

        Ta stored = taDao.findById(TA_ID).orElseThrow();
        assertTrue(stored.getAiSummary() == null || stored.getAiSummary().isBlank());
    }

    @Test
    void updateTaPreservesAiSummaryWhenCvPathUnchangedAndContentUnchanged() {
        Ta ta = taDao.findById(TA_ID).orElseThrow();

        profileService.updateTa(ta, false);

        Ta stored = taDao.findById(TA_ID).orElseThrow();
        assertEquals("Strong Java and teaching experience.", stored.getAiSummary());
    }

    @Test
    void updateTaPreservesAiSummaryWhenOnlyAiSummaryChanges() {
        Ta ta = taDao.findById(TA_ID).orElseThrow();
        ta.setAiSummary("Refined one-line applicant summary.");

        profileService.updateTa(ta);

        Ta stored = taDao.findById(TA_ID).orElseThrow();
        assertEquals("Refined one-line applicant summary.", stored.getAiSummary());
        assertEquals("Computer Science", stored.getMajor());
    }

    @Test
    void shouldInvalidateAiSummaryIgnoresAiSummaryColumn() {
        Ta stored = taDao.findById(TA_ID).orElseThrow();
        Ta updated = taDao.findById(TA_ID).orElseThrow();
        updated.setAiSummary("Different cached summary.");

        assertFalse(ProfileService.shouldInvalidateAiSummary(stored, updated, false));
    }

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
