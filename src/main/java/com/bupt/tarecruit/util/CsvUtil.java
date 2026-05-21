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
 * Utility methods for reading and writing CSV-backed data stores.
 * <p>
 * All file operations use UTF-8 encoding. I/O failures are wrapped in
 * {@link IllegalStateException} rather than propagated as checked exceptions.
 * </p>
 */
public final class CsvUtil {

    private CsvUtil() {
    }

    /**
     * Ensures that the parent directory exists and that the CSV file at {@code path}
     * exists with the given header row. Creates the file when it is missing.
     *
     * @param path   target CSV file path
     * @param header column names written as the first row when the file is created
     */
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

    /**
     * Reads all data rows from a CSV file, excluding the header row.
     *
     * @param path path of the CSV file to read
     * @return list of data rows (never {@code null}; may be empty)
     */
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

    /**
     * Replaces the entire contents of a CSV file with a header row followed by
     * the supplied data rows. Writes to a temporary file first, then atomically
     * replaces the target file.
     *
     * @param path   target CSV file path
     * @param header column names written as the first row
     * @param rows   data rows to persist (excluding the header)
     */
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

    /**
     * Ensures that the parent directory and the file at {@code path} exist,
     * creating them when necessary.
     *
     * @param path target file path whose parent and file must exist
     */
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
