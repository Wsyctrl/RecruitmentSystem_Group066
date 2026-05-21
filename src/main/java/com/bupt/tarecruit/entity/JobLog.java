package com.bupt.tarecruit.entity;

import java.time.LocalDateTime;

/**
 * Audit log entry recording an administrative action performed on a job posting.
 * Captures who performed the action, which job was affected, and the state
 * transition before and after the change.
 */
public class JobLog {

    /**
     * Unique identifier of this log entry.
     */
    private String logId;

    /**
     * Identifier of the administrator who performed the action.
     */
    private String adminId;

    /**
     * Identifier of the job posting that was modified.
     */
    private String jobId;

    /**
     * Type of administrative action that was performed on the job.
     */
    private JobLogAction action;

    /**
     * Serialized job state before the action, for audit purposes.
     */
    private String previousState;

    /**
     * Serialized job state after the action, for audit purposes.
     */
    private String newState;

    /**
     * Date and time when the action was recorded.
     */
    private LocalDateTime timestamp;

    /**
     * Returns the unique identifier of this log entry.
     *
     * @return log entry identifier
     */
    public String getLogId() {
        return logId;
    }

    /**
     * Sets the unique identifier of this log entry.
     *
     * @param logId log entry identifier
     */
    public void setLogId(String logId) {
        this.logId = logId;
    }

    /**
     * Returns the identifier of the administrator who performed the action.
     *
     * @return administrator identifier
     */
    public String getAdminId() {
        return adminId;
    }

    /**
     * Sets the identifier of the administrator who performed the action.
     *
     * @param adminId administrator identifier
     */
    public void setAdminId(String adminId) {
        this.adminId = adminId;
    }

    /**
     * Returns the identifier of the job posting that was modified.
     *
     * @return job identifier
     */
    public String getJobId() {
        return jobId;
    }

    /**
     * Sets the identifier of the job posting that was modified.
     *
     * @param jobId job identifier
     */
    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    /**
     * Returns the type of administrative action that was performed.
     *
     * @return job log action type
     */
    public JobLogAction getAction() {
        return action;
    }

    /**
     * Sets the type of administrative action that was performed.
     *
     * @param action job log action type
     */
    public void setAction(JobLogAction action) {
        this.action = action;
    }

    /**
     * Returns the serialized job state before the action.
     *
     * @return previous state snapshot
     */
    public String getPreviousState() {
        return previousState;
    }

    /**
     * Sets the serialized job state before the action.
     *
     * @param previousState previous state snapshot
     */
    public void setPreviousState(String previousState) {
        this.previousState = previousState;
    }

    /**
     * Returns the serialized job state after the action.
     *
     * @return new state snapshot
     */
    public String getNewState() {
        return newState;
    }

    /**
     * Sets the serialized job state after the action.
     *
     * @param newState new state snapshot
     */
    public void setNewState(String newState) {
        this.newState = newState;
    }

    /**
     * Returns the date and time when the action was recorded.
     *
     * @return action timestamp
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the date and time when the action was recorded.
     *
     * @param timestamp action timestamp
     */
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Returns a human-readable label for the {@link #action}, suitable for UI display.
     *
     * @return display label of the action
     */
    public String getActionLabel() {
        return action.getLabel();
    }

    /**
     * Returns a shortened, UI-friendly string representation of {@link #timestamp}.
     * Replaces the ISO {@code T} separator with a space and truncates to at most
     * 16 characters; returns {@code "-"} when the timestamp is {@code null}.
     *
     * @return formatted timestamp label, or {@code "-"} if absent
     */
    public String getTimestampLabel() {
        return timestamp != null ? timestamp.toString().replace("T", " ").substring(0, Math.min(16, timestamp.toString().length())) : "-";
    }

    /**
     * Types of administrative actions that can be logged against job postings.
     */
    public enum JobLogAction {

        /**
         * The job posting was closed to new applications.
         */
        CLOSE_JOB("Closed"),

        /**
         * The job posting was reopened to accept applications.
         */
        OPEN_JOB("Opened");

        /**
         * Human-readable label shown in the user interface.
         */
        private final String label;

        /**
         * Associates this action constant with its display label.
         *
         * @param label human-readable label for UI rendering
         */
        JobLogAction(String label) {
            this.label = label;
        }

        /**
         * Returns the human-readable label for this action.
         *
         * @return display label
         */
        public String getLabel() {
            return label;
        }
    }
}
