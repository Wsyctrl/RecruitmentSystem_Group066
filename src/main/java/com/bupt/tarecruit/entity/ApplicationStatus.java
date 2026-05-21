package com.bupt.tarecruit.entity;

/**
 * Lifecycle states for a teaching assistant's job application.
 * Each constant has a persisted numeric {@link #code} and a UI {@link #label}.
 */
public enum ApplicationStatus {

    /**
     * Application submitted and awaiting module organizer decision.
     */
    PENDING(0, "Pending"),

    /**
     * Application withdrawn by the teaching assistant.
     */
    WITHDRAWN(1, "Withdrawn"),

    /**
     * Applicant accepted and hired for the position.
     */
    HIRED(2, "Hired"),

    /**
     * Application reviewed and the applicant was not selected.
     */
    REJECTED(3, "Not hired");

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
    ApplicationStatus(int code, String label) {
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
     * Resolves an {@link ApplicationStatus} from its persisted numeric code.
     * Returns {@link #PENDING} when no matching constant is found.
     *
     * @param code numeric status code from storage
     * @return matching status, or {@link #PENDING} as the default fallback
     */
    public static ApplicationStatus fromCode(int code) {
        for (ApplicationStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return PENDING;
    }
}
