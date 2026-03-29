package com.bupt.tarecruit.viewmodel;

import com.bupt.tarecruit.entity.ApplicationRecord;
import com.bupt.tarecruit.entity.Ta;
import com.bupt.tarecruit.util.DateTimeUtil;

public class ApplicantDisplay {

    private final ApplicationRecord record;
    private final Ta ta;

    public ApplicantDisplay(ApplicationRecord record, Ta ta) {
        this.record = record;
        this.ta = ta;
    }

    public ApplicationRecord getRecord() {
        return record;
    }

    public Ta getTa() {
        return ta;
    }

    public String getTaId() {
        return ta != null ? ta.getTaId() : "";
    }

    /** Table column: display name */
    public String getTaName() {
        if (ta == null) {
            return "";
        }
        return ta.getDisplayLabel();
    }

    public String getPhone() {
        return ta != null ? ta.getPhone() : "";
    }

    public String getEmail() {
        return ta != null ? ta.getEmail() : "";
    }

    public String getStatus() {
        return record.getStatus().getLabel();
    }

    public String getUpdatedTime() {
        return DateTimeUtil.formatDateTime(record.getUpdateTime());
    }
}
