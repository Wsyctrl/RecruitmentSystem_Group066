package com.bupt.tarecruit.viewmodel;

import com.bupt.tarecruit.entity.AccountLog;
import com.bupt.tarecruit.util.DateTimeUtil;

/**
 * View model for presenting {@link AccountLog} entries in administrator audit tables.
 * Exposes human-readable labels and identifiers as table-column properties.
 */
public class AccountLogDisplay {

    /** Underlying account log entity. */
    private final AccountLog log;

    /**
     * Creates a display wrapper for the given account log.
     *
     * @param log account log entity; must not be {@code null}
     */
    public AccountLogDisplay(AccountLog log) {
        this.log = log;
    }

    /**
     * Table column: unique log identifier.
     *
     * @return log ID from the entity
     */
    public String getLogId() {
        return log.getLogId();
    }

    /**
     * Table column: administrator who performed the action.
     *
     * @return admin user ID
     */
    public String getAdminId() {
        return log.getAdminId();
    }

    /**
     * Table column: user affected by the operation.
     *
     * @return target user ID
     */
    public String getTargetUserId() {
        return log.getTargetUserId();
    }

    /**
     * Table column: role of the affected user (display label).
     *
     * @return localized or formatted target role label
     */
    public String getTargetRole() {
        return log.getTargetRoleLabel();
    }

    /**
     * Table column: type of account operation (display label).
     *
     * @return localized or formatted action label
     */
    public String getAction() {
        return log.getActionLabel();
    }

    /**
     * Table column: account state before the change.
     *
     * @return previous state description, or empty if not applicable
     */
    public String getPreviousState() {
        return log.getPreviousState();
    }

    /**
     * Table column: account state after the change.
     *
     * @return new state description, or empty if not applicable
     */
    public String getNewState() {
        return log.getNewState();
    }

    /**
     * Table column: when the operation occurred (formatted for display).
     *
     * @return formatted timestamp label
     */
    public String getTimestamp() {
        return log.getTimestampLabel();
    }

    /**
     * Returns the wrapped account log entity for detail views or further processing.
     *
     * @return underlying {@link AccountLog}
     */
    public AccountLog getLog() {
        return log;
    }
}
