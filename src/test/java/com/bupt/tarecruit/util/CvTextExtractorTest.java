package com.bupt.tarecruit.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CvTextExtractor}.
 */
class CvTextExtractorTest {

    @TempDir
    Path tempDir;

    /** Verifies plain text and markdown files are read as UTF-8 strings. */
    @Test
    void extractTextFromTxtAndMarkdown() throws Exception {
        Path txt = tempDir.resolve("resume.txt");
        Path md = tempDir.resolve("resume.md");
        Files.writeString(txt, "Name: Alice");
        Files.writeString(md, "## Skills\nJava");

        assertEquals("Name: Alice", CvTextExtractor.extractText(txt));
        assertEquals("## Skills\nJava", CvTextExtractor.extractText(md));
    }

    /** Verifies PDF resume text can be extracted for AI features. */
    @Test
    void extractTextFromPdf() throws Exception {
        Path pdf = tempDir.resolve("resume.pdf");
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText("Alice Wang");
                contentStream.endText();
            }
            document.save(pdf.toFile());
        }

        assertTrue(CvTextExtractor.extractText(pdf).contains("Alice Wang"));
    }
}
