package com.bupt.tarecruit.util;

/**
 * Outcome of persisting a teaching assistant CV attachment to disk.
 * <p>
 * {@code relativePath} reflects the stored format ({@code .txt}, {@code .md}, or {@code .pdf})
 * under {@code data/cv/{email}_cv{extension}}.
 * </p>
 *
 * @param relativePath   path written to {@code TA.csv} ({@code cv_path}), relative to the project root
 * @param contentChanged {@code true} when the file was newly created or its bytes changed;
 *                       {@code false} when an identical file already existed
 */
public record CvSaveOutcome(String relativePath, boolean contentChanged) {
}
