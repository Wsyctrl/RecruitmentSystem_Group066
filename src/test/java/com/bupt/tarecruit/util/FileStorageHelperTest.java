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
        assertEquals("ta@bupt.edu.cn_cv.pdf", FileStorageHelper.cvFileName("ta@bupt.edu.cn", ".pdf"));
        assertEquals("data/cv/ta@bupt.edu.cn_cv.md", FileStorageHelper.cvRelativePath("ta@bupt.edu.cn", ".md"));
    }

    /** Verifies cv file name blank email throws. */
    @Test
    void cvFileNameBlankEmailThrows() {
        assertThrows(IllegalArgumentException.class, () -> FileStorageHelper.cvFileName(" "));
    }

    /** Verifies allowed extension checks. */
    @Test
    void allowedCvExtensions() {
        assertTrue(FileStorageHelper.isAllowedCvFileName("resume.txt"));
        assertTrue(FileStorageHelper.isAllowedCvFileName("resume.MD"));
        assertTrue(FileStorageHelper.isAllowedCvFileName("resume.pdf"));
        assertFalse(FileStorageHelper.isAllowedCvFileName("resume.docx"));
        assertEquals(".pdf", FileStorageHelper.extensionOf("Alice_cv.pdf"));
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

    /** Verifies switching CV format removes the previous extension. */
    @Test
    void saveCvReplacesOtherFormats() throws Exception {
        FileStorageHelper helper = new FileStorageHelper(tempDir);
        Path txtSource = tempDir.resolve("upload.txt");
        Path pdfSource = tempDir.resolve("upload.pdf");
        Files.writeString(txtSource, "txt content");
        Files.writeString(pdfSource, "pdf content");

        helper.saveCv("ta@bupt.edu.cn", txtSource.toFile());
        Path txtFile = helper.getCvDir().resolve(FileStorageHelper.cvFileName("ta@bupt.edu.cn", ".txt"));
        assertTrue(Files.exists(txtFile));

        CvSaveOutcome pdfOutcome = helper.saveCv("ta@bupt.edu.cn", pdfSource.toFile());
        assertEquals("data/cv/ta@bupt.edu.cn_cv.pdf", pdfOutcome.relativePath());
        assertFalse(Files.exists(txtFile));
        assertTrue(Files.exists(helper.getCvDir().resolve(FileStorageHelper.cvFileName("ta@bupt.edu.cn", ".pdf"))));
    }

    /** Verifies markdown CV text can be read back for AI features. */
    @Test
    void readCvTextFromMarkdown() throws Exception {
        FileStorageHelper helper = new FileStorageHelper(tempDir);
        Path source = tempDir.resolve("upload.md");
        Files.writeString(source, "# Alice\n\nSkills: Java");
        CvSaveOutcome outcome = helper.saveCv("ta@bupt.edu.cn", source.toFile());

        assertEquals("# Alice\n\nSkills: Java", helper.readCvText("ta@bupt.edu.cn", outcome.relativePath()));
    }
}
