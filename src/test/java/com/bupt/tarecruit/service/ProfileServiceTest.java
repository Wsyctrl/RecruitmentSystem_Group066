package com.bupt.tarecruit.service;

import com.bupt.tarecruit.dao.CsvMoDao;
import com.bupt.tarecruit.dao.CsvTaDao;
import com.bupt.tarecruit.entity.Ta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
        ta.setAiSummary("Strong Java and teaching experience.");
        taDao.save(ta);
    }

    @Test
    void updateTaClearsAiSummaryWhenProfileFieldsChange() {
        Ta ta = taDao.findById(TA_ID).orElseThrow();
        ta.setMajor("Software Engineering");

        profileService.updateTa(ta);

        Ta stored = taDao.findById(TA_ID).orElseThrow();
        assertEquals("Software Engineering", stored.getMajor());
        assertTrue(stored.getAiSummary() == null || stored.getAiSummary().isBlank());
    }

    @Test
    void updateTaClearsAiSummaryWhenCvPathChanges() {
        Ta ta = taDao.findById(TA_ID).orElseThrow();
        ta.setCvPath("cv/ta@bupt.edu.cn/resume.txt");

        profileService.updateTa(ta);

        Ta stored = taDao.findById(TA_ID).orElseThrow();
        assertEquals("cv/ta@bupt.edu.cn/resume.txt", stored.getCvPath());
        assertTrue(stored.getAiSummary() == null || stored.getAiSummary().isBlank());
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
    void hasNonAiSummaryFieldChangedIgnoresAiSummaryColumn() {
        Ta stored = taDao.findById(TA_ID).orElseThrow();
        Ta updated = taDao.findById(TA_ID).orElseThrow();
        updated.setAiSummary("Different cached summary.");

        assertFalse(ProfileService.hasNonAiSummaryFieldChanged(stored, updated));
    }
}
