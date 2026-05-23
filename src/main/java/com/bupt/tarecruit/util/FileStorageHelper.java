package com.bupt.tarecruit.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Manages on-disk storage for teaching assistant CV files under the application data directory.
 * <p>
 * Attachments are stored under {@code data/cv/} as {@code {email}_cv.{txt|md|pdf}}. Upload,
 * download, delete, and AI text extraction all use the same extension set defined by
 * {@link #ALLOWED_CV_EXTENSIONS}. Plain text for LLM prompts is produced by
 * {@link CvTextExtractor}.
 * </p>
 */
public class FileStorageHelper {

    /**
     * Supported resume attachment extensions (lowercase, including the leading dot).
     * <p>Values: {@code .txt}, {@code .md}, {@code .pdf}.</p>
     */
    public static final List<String> ALLOWED_CV_EXTENSIONS = List.of(".txt", ".md", ".pdf");

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
     * @throws IllegalStateException when the directory cannot be created
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
        return cvFileName(email, ".txt");
    }

    /**
     * Builds the canonical CV file name for a teaching assistant email and extension.
     *
     * @param email     teaching assistant email (used as identity)
     * @param extension file extension such as {@code .txt}, {@code .md}, or {@code .pdf}
     * @return file name in the form {@code {email}_cv{extension}}
     * @throws IllegalArgumentException when {@code email} is null or blank, or the extension is unsupported
     */
    public static String cvFileName(String email, String extension) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email is required");
        }
        return email.trim() + "_cv" + normalizeExtension(extension);
    }

    /**
     * Returns the relative CV path stored in {@code TA.csv} ({@code cv_path}).
     *
     * @param email teaching assistant email
     * @return path relative to the project root, e.g. {@code data/cv/{email}_cv.txt}
     */
    public static String cvRelativePath(String email) {
        return cvRelativePath(email, ".txt");
    }

    /**
     * Returns the relative CV path stored in {@code TA.csv} ({@code cv_path}).
     *
     * @param email     teaching assistant email
     * @param extension file extension such as {@code .txt}, {@code .md}, or {@code .pdf}
     * @return path relative to the project root
     */
    public static String cvRelativePath(String email, String extension) {
        return "data/cv/" + cvFileName(email, extension);
    }

    /**
     * Checks whether the given file name uses a supported resume attachment extension.
     *
     * @param fileName source file name; null or blank yields {@code false}
     * @return {@code true} when the extension is {@code .txt}, {@code .md}, or {@code .pdf}
     */
    public static boolean isAllowedCvFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return false;
        }
        return ALLOWED_CV_EXTENSIONS.stream().anyMatch(ext -> fileName.toLowerCase(Locale.ROOT).endsWith(ext));
    }

    /**
     * Returns the normalized extension for a file name.
     *
     * @param fileName source file name; null or blank throws {@link IllegalArgumentException}
     * @return extension including the leading dot
     * @throws IllegalArgumentException when the file name is missing or the extension is unsupported
     */
    public static String extensionOf(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("file name is required");
        }
        String lower = fileName.toLowerCase(Locale.ROOT);
        for (String ext : ALLOWED_CV_EXTENSIONS) {
            if (lower.endsWith(ext)) {
                return ext;
            }
        }
        throw new IllegalArgumentException("Unsupported CV file type: " + fileName);
    }

    /**
     * Copies CV content from a source file into the canonical CV location for the given email.
     * The stored extension is taken from {@link #extensionOf(String)} on the source file name.
     * Any previously stored CV file for the same email in another supported format is removed.
     * Skips writing when an existing file already has identical bytes.
     *
     * @param email  teaching assistant email
     * @param source source file to read; {@code null} yields a result with no path and no change
     * @return save outcome including relative path and whether content changed
     * @throws IllegalArgumentException when the source file name has an unsupported extension
     * @throws IllegalStateException when the file cannot be read or written
     */
    public CvSaveOutcome saveCv(String email, File source) {
        if (source == null) {
            return new CvSaveOutcome(null, false);
        }
        String extension = extensionOf(source.getName());
        Path target = getCvDir().resolve(cvFileName(email, extension));
        try {
            deleteOtherCvFormats(email, extension);
            byte[] newContent = Files.readAllBytes(source.toPath());
            boolean contentChanged = true;
            if (Files.isRegularFile(target)) {
                byte[] existingContent = Files.readAllBytes(target);
                contentChanged = !Arrays.equals(existingContent, newContent);
            }
            if (contentChanged) {
                Files.write(target, newContent);
            }
            return new CvSaveOutcome(cvRelativePath(email, extension), contentChanged);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save CV", e);
        }
    }

    /**
     * Deletes all canonical CV files for the email ({@code .txt}, {@code .md}, {@code .pdf})
     * and, when different, the file at {@code storedPath}.
     *
     * @param email      teaching assistant email
     * @param storedPath path from CSV ({@code cv_path}); may be null or blank
     * @throws IOException when a file deletion fails
     */
    public void deleteCv(String email, String storedPath) throws IOException {
        for (String extension : ALLOWED_CV_EXTENSIONS) {
            deleteIfExists(getCvDir().resolve(cvFileName(email, extension)));
        }
        if (storedPath != null && !storedPath.isBlank()) {
            Path resolved = resolveCvFile(email, storedPath);
            deleteIfExists(resolved);
        }
    }

    /**
     * Reads resume text for AI features from the stored CV attachment.
     * <p>
     * Supports {@code .txt}, {@code .md}, and {@code .pdf} via {@link CvTextExtractor}.
     * IO or parse failures are swallowed and yield an empty string so callers can continue.
     * </p>
     *
     * @param email      teaching assistant email
     * @param storedPath path from CSV ({@code cv_path}); may be null or blank
     * @return extracted text, or an empty string when no readable CV is available
     */
    public String readCvText(String email, String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return "";
        }
        Path cvFile = resolveCvFile(email, storedPath);
        if (!Files.isRegularFile(cvFile)) {
            return "";
        }
        try {
            return CvTextExtractor.extractText(cvFile);
        } catch (IOException e) {
            return "";
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
     * Removes CV files for the email in formats other than the one being saved.
     *
     * @param email         teaching assistant email
     * @param keepExtension extension to retain (e.g. {@code .pdf})
     * @throws IOException when deletion of an existing file fails
     */
    private void deleteOtherCvFormats(String email, String keepExtension) throws IOException {
        for (String extension : ALLOWED_CV_EXTENSIONS) {
            if (extension.equals(keepExtension)) {
                continue;
            }
            deleteIfExists(getCvDir().resolve(cvFileName(email, extension)));
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
     * Falls back to the first existing canonical CV file under {@code cv/}, then {@code .txt}.
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
        for (String extension : ALLOWED_CV_EXTENSIONS) {
            Path candidate = getCvDir().resolve(cvFileName(email, extension));
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return getCvDir().resolve(cvFileName(email));
    }

    /**
     * Normalizes and validates a CV file extension.
     *
     * @param extension extension with or without a leading dot (e.g. {@code pdf} or {@code .PDF})
     * @return lowercase extension including the leading dot
     * @throws IllegalArgumentException when {@code extension} is null, blank, or unsupported
     */
    private static String normalizeExtension(String extension) {
        if (extension == null || extension.isBlank()) {
            throw new IllegalArgumentException("extension is required");
        }
        String normalized = extension.startsWith(".")
                ? extension.toLowerCase(Locale.ROOT)
                : "." + extension.toLowerCase(Locale.ROOT);
        if (!ALLOWED_CV_EXTENSIONS.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported CV extension: " + extension);
        }
        return normalized;
    }
}
