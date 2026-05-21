package com.bupt.tarecruit.entity;

/**
 * Open or closed state of a job posting with respect to accepting applications.
 * Each constant has a persisted numeric {@link #code} and a UI {@link #label}.
 */
public enum JobStatus {

    /**
     * The posting is active and accepts new applications.
     */
    OPEN(0, "Open"),

    /**
     * The posting is inactive and does not accept new applications.
     */
    CLOSED(1, "Closed");

    /**
     * Numeric code stored in CSV persistence.
     */
    private final int code;

    /**
     * Human-readable label shown in the user interface.
     */
    private final String label;

    /**
     * Associates this status constant with its persisted code and display label.
     *
     * @param code  numeric code for CSV storage
     * @param label human-readable label for UI rendering
     */
    JobStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    /**
     * Returns the numeric code used when persisting this status to CSV.
     *
     * @return persisted status code
     */
    public int getCode() {
        return code;
    }

    /**
     * Returns the human-readable label for this status.
     *
     * @return display label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Resolves a {@link JobStatus} from its persisted numeric code.
     * Returns {@link #CLOSED} when the code is {@code 1}, otherwise {@link #OPEN}.
     *
     * @param code numeric status code from storage
     * @return {@link #CLOSED} if code is {@code 1}, otherwise {@link #OPEN}
     */
    public static JobStatus fromCode(int code) {
        return code == 1 ? CLOSED : OPEN;
    }
}
