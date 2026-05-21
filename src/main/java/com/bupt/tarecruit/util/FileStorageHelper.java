package com.bupt.tarecruit.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

/**
 * Manages on-disk storage for teaching assistant CV files under the application data directory.
 */
public class FileStorageHelper {

    /**
     * Root directory for application data (CSV files and the {@code cv} subdirectory).
     */
    private final Path dataDir;

    /**
     * Creates a helper bound to the given data directory.
     *
     * @param dataDir root path of the data store
     */
    public FileStorageHelper(Path dataDir) {
        this.dataDir = dataDir;
    }

    /**
     * Returns the CV storage directory, creating it when missing.
     *
     * @return absolute path to the {@code cv} subdirectory under {@code dataDir}
     */
    public Path getCvDir() {
        Path cvDir = dataDir.resolve("cv");
        try {
            Files.createDirectories(cvDir);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create CV directory", e);
        }
        return cvDir;
    }

    /**
     * Builds the canonical CV file name for a teaching assistant email.
     *
     * @param email teaching assistant email (used as identity)
     * @return file name in the form {@code {email}_cv.txt}
     * @throws IllegalArgumentException when {@code email} is null or blank
     */
    public static String cvFileName(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email is required");
        }
        return email.trim() + "_cv.txt";
    }

    /**
     * Returns the relative CV path stored in {@code TA.csv} ({@code cv_path}).
     *
     * @param email teaching assistant email
     * @return path relative to the project root, e.g. {@code data/cv/{email}_cv.txt}
     */
    public static String cvRelativePath(String email) {
        return "data/cv/" + cvFileName(email);
    }

    /**
     * Copies CV content from a source file into the canonical CV location for the given email.
     * Skips writing when an existing file already has identical bytes.
     *
     * @param email  teaching assistant email
     * @param source source file to read; {@code null} yields a result with no path and no change
     * @return save outcome including relative path and whether content changed
     */
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

    /**
     * Deletes the canonical CV file for the email and, when different, the file at {@code storedPath}.
     *
     * @param email      teaching assistant email
     * @param storedPath path from CSV ({@code cv_path}); may be null or blank
     * @throws IOException when a file deletion fails
     */
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

    /**
     * Deletes a file when it exists.
     *
     * @param path file path to delete
     * @throws IOException when deletion fails
     */
    private void deleteIfExists(Path path) throws IOException {
        if (Files.exists(path)) {
            Files.delete(path);
        }
    }

    /**
     * Resolves a path under the data directory.
     *
     * @param first first path segment
     * @param more  additional path segments
     * @return resolved path under {@code dataDir}
     */
    public Path resolve(String first, String... more) {
        return dataDir.resolve(Path.of(first, more));
    }

    /**
     * Returns the root data directory.
     *
     * @return data directory path
     */
    public Path getDataDir() {
        return dataDir;
    }

    /**
     * Resolves a stored CV path from CSV (may be relative or use mixed separators).
     * Falls back to {@code cv/{email}_cv.txt} under the data directory when no file matches.
     *
     * @param email      teaching assistant email; blank values are treated as {@code unknown}
     * @param storedPath path from CSV ({@code cv_path}); may be null or blank
     * @return first existing file path, or the canonical fallback under {@code getCvDir()}
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
