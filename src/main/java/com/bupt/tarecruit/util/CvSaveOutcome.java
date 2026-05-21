package com.bupt.tarecruit.util;

/**
 * Result of saving a TA CV attachment.
 *
 * @param relativePath   stored path written to TA.csv ({@code cv_path})
 * @param contentChanged {@code true} when the file was newly created or its bytes changed
 */
public record CvSaveOutcome(String relativePath, boolean contentChanged) {
}
