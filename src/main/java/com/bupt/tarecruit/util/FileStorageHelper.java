package com.bupt.tarecruit.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class FileStorageHelper {

    private final Path dataDir;

    public FileStorageHelper(Path dataDir) {
        this.dataDir = dataDir;
    }

    public Path getCvDir() {
        Path cvDir = dataDir.resolve("cv");
        try {
            Files.createDirectories(cvDir);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create CV directory", e);
        }
        return cvDir;
    }

    public String saveCv(String taId, File source) {
        if (source == null) {
            return null;
        }
        Path target = getCvDir().resolve(taId + "_cv.txt");
        try {
            Files.copy(source.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
            return target.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save CV", e);
        }
    }

    public Path resolve(String first, String... more) {
        return dataDir.resolve(Path.of(first, more));
    }

    public Path getDataDir() {
        return dataDir;
    }

    /**
     * Resolves a stored CV path from CSV (may be relative or use mixed separators).
     * Falls back to {@code cv/{taId}_cv.txt} under the data directory.
     */
    public Path resolveCvFile(String taId, String storedPath) {
        if (taId == null || taId.isBlank()) {
            taId = "unknown";
        }
        Path cwd = Path.of("").toAbsolutePath();
        if (storedPath != null && !storedPath.isBlank()) {
            String normalized = storedPath.trim().replace('\\', '/');
            Path fromCwd = cwd.resolve(normalized);
            if (Files.isRegularFile(fromCwd)) {
                return fromCwd;
            }
            Path direct = Path.of(normalized);
            if (Files.isRegularFile(direct)) {
                return direct;
            }
            String relativeToDataDir = normalized.startsWith("data/")
                    ? normalized.substring("data/".length())
                    : normalized;
            Path underData = dataDir.resolve(relativeToDataDir);
            if (Files.isRegularFile(underData)) {
                return underData;
            }
        }
        return getCvDir().resolve(taId + "_cv.txt");
    }
}
