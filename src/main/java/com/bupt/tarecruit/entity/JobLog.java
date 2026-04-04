package com.bupt.tarecruit.entity;

import java.time.LocalDateTime;

public class JobLog {
    private String logId;
    private String adminId;
    private String jobId;
    private JobLogAction action;
    private String previousState;
    private String newState;
    private LocalDateTime timestamp;

    public String getLogId() {
        return logId;
    }

    public void setLogId(String logId) {
        this.logId = logId;
    }

    public String getAdminId() {
        return adminId;
    }

    public void setAdminId(String adminId) {
        this.adminId = adminId;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public JobLogAction getAction() {
        return action;
    }

    public void setAction(JobLogAction action) {
        this.action = action;
    }

    public String getPreviousState() {
        return previousState;
    }

    public void setPreviousState(String previousState) {
        this.previousState = previousState;
    }

    public String getNewState() {
        return newState;
    }

    public void setNewState(String newState) {
        this.newState = newState;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getActionLabel() {
        return action.getLabel();
    }

    public String getTimestampLabel() {
        return timestamp != null ? timestamp.toString().replace("T", " ").substring(0, Math.min(16, timestamp.toString().length())) : "-";
    }

    public enum JobLogAction {
        CLOSE_JOB("Closed"),
        OPEN_JOB("Opened");

        private final String label;

        JobLogAction(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }
}
