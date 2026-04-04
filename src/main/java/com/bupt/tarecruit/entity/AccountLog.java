package com.bupt.tarecruit.entity;

import java.time.LocalDateTime;

public class AccountLog {
    private String logId;
    private String adminId;
    private String targetUserId;
    private Role targetRole;
    private AccountAction action;
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

    public String getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(String targetUserId) {
        this.targetUserId = targetUserId;
    }

    public Role getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(Role targetRole) {
        this.targetRole = targetRole;
    }

    public AccountAction getAction() {
        return action;
    }

    public void setAction(AccountAction action) {
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

    public String getTargetRoleLabel() {
        return targetRole == Role.TA ? "TA" : "MO";
    }

    public enum AccountAction {
        DISABLE("Disabled"),
        ENABLE("Enabled"),
        RESET_PASSWORD("Reset Password");

        private final String label;

        AccountAction(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }
}
