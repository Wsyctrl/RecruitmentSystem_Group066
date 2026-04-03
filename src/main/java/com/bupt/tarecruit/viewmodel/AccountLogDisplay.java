package com.bupt.tarecruit.viewmodel;

import com.bupt.tarecruit.entity.AccountLog;
import com.bupt.tarecruit.util.DateTimeUtil;

public class AccountLogDisplay {
    private final AccountLog log;

    public AccountLogDisplay(AccountLog log) {
        this.log = log;
    }

    public String getLogId() {
        return log.getLogId();
    }

    public String getAdminId() {
        return log.getAdminId();
    }

    public String getTargetUserId() {
        return log.getTargetUserId();
    }

    public String getTargetRole() {
        return log.getTargetRoleLabel();
    }

    public String getAction() {
        return log.getActionLabel();
    }

    public String getPreviousState() {
        return log.getPreviousState();
    }

    public String getNewState() {
        return log.getNewState();
    }

    public String getTimestamp() {
        return log.getTimestampLabel();
    }

    public AccountLog getLog() {
        return log;
    }
}
