package com.bupt.tarecruit.util;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Extracts plain text from TA resume attachments stored as {@code .txt}, {@code .md}, or {@code .pdf}.
 */
public final class CvTextExtractor {

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

    private static String extractPdfText(Path cvFile) throws IOException {
        try (PDDocument document = Loader.loadPDF(cvFile.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
}
