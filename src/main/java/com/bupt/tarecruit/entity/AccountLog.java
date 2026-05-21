package com.bupt.tarecruit.entity;

import java.time.LocalDateTime;

/**
 * Audit log entry recording an administrative action performed on a user account.
 * Captures who performed the action, which account was affected, and the state
 * transition before and after the change.
 */
public class AccountLog {

    /**
     * Unique identifier of this log entry.
     */
    private String logId;

    /**
     * Identifier of the administrator who performed the action.
     */
    private String adminId;

    /**
     * Identifier of the user account that was modified.
     */
    private String targetUserId;

    /**
     * Role of the target user ({@link Role#TA} or {@link Role#MO}).
     */
    private Role targetRole;

    /**
     * Type of administrative action that was performed.
     */
    private AccountAction action;

    /**
     * Serialized account state before the action, for audit purposes.
     */
    private String previousState;

    /**
     * Serialized account state after the action, for audit purposes.
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
     * Returns the identifier of the user account that was modified.
     *
     * @return target user identifier
     */
    public String getTargetUserId() {
        return targetUserId;
    }

    /**
     * Sets the identifier of the user account that was modified.
     *
     * @param targetUserId target user identifier
     */
    public void setTargetUserId(String targetUserId) {
        this.targetUserId = targetUserId;
    }

    /**
     * Returns the role of the target user.
     *
     * @return target user role
     */
    public Role getTargetRole() {
        return targetRole;
    }

    /**
     * Sets the role of the target user.
     *
     * @param targetRole target user role
     */
    public void setTargetRole(Role targetRole) {
        this.targetRole = targetRole;
    }

    /**
     * Returns the type of administrative action that was performed.
     *
     * @return account action type
     */
    public AccountAction getAction() {
        return action;
    }

    /**
     * Sets the type of administrative action that was performed.
     *
     * @param action account action type
     */
    public void setAction(AccountAction action) {
        this.action = action;
    }

    /**
     * Returns the serialized account state before the action.
     *
     * @return previous state snapshot
     */
    public String getPreviousState() {
        return previousState;
    }

    /**
     * Sets the serialized account state before the action.
     *
     * @param previousState previous state snapshot
     */
    public void setPreviousState(String previousState) {
        this.previousState = previousState;
    }

    /**
     * Returns the serialized account state after the action.
     *
     * @return new state snapshot
     */
    public String getNewState() {
        return newState;
    }

    /**
     * Sets the serialized account state after the action.
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
     * Returns a short role label for the target user: {@code "TA"} when
     * {@link #targetRole} is {@link Role#TA}, otherwise {@code "MO"}.
     *
     * @return target role display label
     */
    public String getTargetRoleLabel() {
        return targetRole == Role.TA ? "TA" : "MO";
    }

    /**
     * Types of administrative actions that can be logged against user accounts.
     */
    public enum AccountAction {

        /**
         * The target account was disabled.
         */
        DISABLE("Disabled"),

        /**
         * The target account was re-enabled.
         */
        ENABLE("Enabled"),

        /**
         * The target account password was reset by an administrator.
         */
        RESET_PASSWORD("Reset Password");

        /**
         * Human-readable label shown in the user interface.
         */
        private final String label;

        /**
         * Associates this action constant with its display label.
         *
         * @param label human-readable label for UI rendering
         */
        AccountAction(String label) {
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
