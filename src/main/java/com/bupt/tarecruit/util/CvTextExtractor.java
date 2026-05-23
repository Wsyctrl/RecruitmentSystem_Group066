package com.bupt.tarecruit.util;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Extracts plain text from TA resume attachments stored as {@code .txt}, {@code .md}, or {@code .pdf}.
 * <p>
 * Used by {@link FileStorageHelper#readCvText} and AI fill-from-CV flows so large-language-model
 * prompts always receive a UTF-8 string regardless of the on-disk attachment format.
 * </p>
 */
public final class CvTextExtractor {

    /** Utility class; not instantiable. */
    private CvTextExtractor() {
    }

    /**
     * Reads resume body text from a file on disk.
     *
     * @param cvFile path to the stored CV file
     * @return extracted text, or an empty string when the file is empty
     * @throws IOException when the file cannot be read or parsed
     */
    public static String extractText(Path cvFile) throws IOException {
        String lowerName = cvFile.getFileName().toString().toLowerCase();
        if (lowerName.endsWith(".pdf")) {
            return extractPdfText(cvFile);
        }
        return Files.readString(cvFile);
    }

    /**
     * Extracts text from a PDF resume using Apache PDFBox.
     *
     * @param cvFile path to a {@code .pdf} file
     * @return stripped text content from all pages
     * @throws IOException when the PDF cannot be loaded or parsed
     */
    private static String extractPdfText(Path cvFile) throws IOException {
        try (PDDocument document = Loader.loadPDF(cvFile.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
}
