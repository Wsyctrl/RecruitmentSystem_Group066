package com.bupt.tarecruit.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

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

    public static String cvFileName(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email is required");
        }
        return email.trim() + "_cv.txt";
    }

    public static String cvRelativePath(String email) {
        return "data/cv/" + cvFileName(email);
    }

    public CvSaveOutcome saveCv(String email, File source) {
        if (source == null) {
            return new CvSaveOutcome(null, false);
        }
        Path target = getCvDir().resolve(cvFileName(email));
        try {
            byte[] newContent = Files.readAllBytes(source.toPath());
            boolean contentChanged = true;
            if (Files.isRegularFile(target)) {
                byte[] existingContent = Files.readAllBytes(target);
                contentChanged = !Arrays.equals(existingContent, newContent);
            }
            if (contentChanged) {
                Files.write(target, newContent);
            }
            return new CvSaveOutcome(cvRelativePath(email), contentChanged);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save CV", e);
        }
    }

    public void deleteCv(String email, String storedPath) throws IOException {
        Path canonical = getCvDir().resolve(cvFileName(email));
        deleteIfExists(canonical);
        if (storedPath != null && !storedPath.isBlank()) {
            Path resolved = resolveCvFile(email, storedPath);
            if (!resolved.equals(canonical)) {
                deleteIfExists(resolved);
            }
        }
    }

    private void deleteIfExists(Path path) throws IOException {
        if (Files.exists(path)) {
            Files.delete(path);
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
     * Falls back to {@code cv/{email}_cv.txt} under the data directory.
     */
    public Path resolveCvFile(String email, String storedPath) {
        if (email == null || email.isBlank()) {
            email = "unknown";
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
        return getCvDir().resolve(cvFileName(email));
    }
}
