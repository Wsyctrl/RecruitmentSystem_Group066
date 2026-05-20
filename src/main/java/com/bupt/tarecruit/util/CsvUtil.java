package com.bupt.tarecruit.util;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Helper methods to work with CSV-backed data stores.
 */
public final class CsvUtil {

    private CsvUtil() {
    }

    public static void ensureFileWithHeader(Path path, String[] header) {
        try {
            Files.createDirectories(path.getParent());
            if (Files.notExists(path)) {
                try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8);
                     CSVWriter csvWriter = new CSVWriter(writer)) {
                    csvWriter.writeNext(header, false);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not initialize data file: " + path, e);
        }
    }

    public static List<String[]> readDataRows(Path path) {
        ensurePathExists(path);
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
             CSVReader csvReader = new CSVReader(reader)) {
            List<String[]> rows = csvReader.readAll();
            if (!rows.isEmpty()) {
                rows.remove(0); // remove header
            }
            return rows;
        } catch (IOException | CsvException e) {
            throw new IllegalStateException("Failed to read CSV: " + path, e);
        }
    }

    public static void writeAll(Path path, String[] header, List<String[]> rows) {
        ensurePathExists(path);
        Path tempPath = path.resolveSibling(path.getFileName().toString() + ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(tempPath, StandardCharsets.UTF_8);
                 CSVWriter csvWriter = new CSVWriter(writer)) {
                csvWriter.writeNext(header, false);
                for (String[] row : rows) {
                    csvWriter.writeNext(row, false);
                }
            }
            Files.move(tempPath, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            try {
                Files.deleteIfExists(tempPath);
            } catch (IOException ignored) {
            }
            throw new IllegalStateException("Failed to write CSV: " + path, e);
        }
    }

    private static void ensurePathExists(Path path) {
        try {
            if (Files.notExists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }
            if (Files.notExists(path)) {
                Files.createFile(path);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not ensure path exists: " + path, e);
        }
    }
}
