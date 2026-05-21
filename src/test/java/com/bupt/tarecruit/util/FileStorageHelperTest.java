package com.bupt.tarecruit.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FileStorageHelper CV file operations.
 */
class FileStorageHelperTest {

    @TempDir
    Path tempDir;
    /** Verifies cv file name and relative path. */
    @Test
    void cvFileNameAndRelativePath() {
        assertEquals("ta@bupt.edu.cn_cv.txt", FileStorageHelper.cvFileName("ta@bupt.edu.cn"));
        assertEquals("data/cv/ta@bupt.edu.cn_cv.txt", FileStorageHelper.cvRelativePath("ta@bupt.edu.cn"));
    }
    /** Verifies cv file name blank email throws. */
    @Test
    void cvFileNameBlankEmailThrows() {
        assertThrows(IllegalArgumentException.class, () -> FileStorageHelper.cvFileName(" "));
    }
    /** Verifies save cv null source returns no path. */
    @Test
    void saveCvNullSourceReturnsNoPath() {
        FileStorageHelper helper = new FileStorageHelper(tempDir);
        CvSaveOutcome outcome = helper.saveCv("ta@bupt.edu.cn", null);
        assertNull(outcome.relativePath());
        assertFalse(outcome.contentChanged());
    }
    /** Verifies delete cv removes file. */
    @Test
    void deleteCvRemovesFile() throws Exception {
        FileStorageHelper helper = new FileStorageHelper(tempDir);
        Path source = tempDir.resolve("upload.txt");
        Files.writeString(source, "content");
        helper.saveCv("ta@bupt.edu.cn", source.toFile());

        Path cvFile = helper.getCvDir().resolve(FileStorageHelper.cvFileName("ta@bupt.edu.cn"));
        assertTrue(Files.exists(cvFile));

        helper.deleteCv("ta@bupt.edu.cn", FileStorageHelper.cvRelativePath("ta@bupt.edu.cn"));
        assertFalse(Files.exists(cvFile));
    }
}
