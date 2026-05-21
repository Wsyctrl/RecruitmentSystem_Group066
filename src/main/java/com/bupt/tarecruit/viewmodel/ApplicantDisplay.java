package com.bupt.tarecruit.viewmodel;

import com.bupt.tarecruit.entity.ApplicationRecord;
import com.bupt.tarecruit.entity.Ta;
import com.bupt.tarecruit.util.DateTimeUtil;

/**
 * View model for presenting job applicants in MO or administrator applicant tables.
 * Combines an {@link ApplicationRecord} with the applying {@link Ta} for table-column properties.
 */
public class ApplicantDisplay {

    /** Underlying application record. */
    private final ApplicationRecord record;

    /** Teaching assistant who submitted the application; may be {@code null}. */
    private final Ta ta;

    /**
     * Creates a display wrapper for an applicant row.
     *
     * @param record application record; must not be {@code null}
     * @param ta     applying teaching assistant; may be {@code null}
     */
    public ApplicantDisplay(ApplicationRecord record, Ta ta) {
        this.record = record;
        this.ta = ta;
    }

    /**
     * Returns the wrapped application record for hire/reject actions or detail views.
     *
     * @return underlying {@link ApplicationRecord}
     */
    public ApplicationRecord getRecord() {
        return record;
    }

    /**
     * Returns the applying teaching assistant entity.
     *
     * @return underlying {@link Ta}, or {@code null} if not resolved
     */
    public Ta getTa() {
        return ta;
    }

    /**
     * Table column: teaching assistant identifier.
     *
     * @return TA user ID, or empty string if TA is unavailable
     */
    public String getTaId() {
        return ta != null ? ta.getTaId() : "";
    }

    /**
     * Table column: applicant display name (full name or email identity).
     *
     * @return TA display label, or empty string if TA is unavailable
     */
    public String getTaName() {
        if (ta == null) {
            return "";
        }
        return ta.getDisplayLabel();
    }

    /**
     * Table column: applicant contact phone number.
     *
     * @return phone number, or empty string if TA is unavailable
     */
    public String getPhone() {
        return ta != null ? ta.getPhone() : "";
    }

    /**
     * Table column: applicant contact email address.
     *
     * @return email address, or empty string if TA is unavailable
     */
    public String getEmail() {
        return ta != null ? ta.getEmail() : "";
    }

    /**
     * Table column: current application status (display label).
     *
     * @return localized or formatted status label
     */
    public String getStatus() {
        return record.getStatus().getLabel();
    }

    /**
     * Table column: time when the application was submitted.
     *
     * @return formatted apply timestamp
     */
    public String getApplyTime() {
        return DateTimeUtil.formatDateTime(record.getApplyTime());
    }

    /**
     * Table column: time when the applicant was hired, if applicable.
     *
     * @return formatted hire timestamp, or {@code "-"} if not hired
     */
    public String getHiredTime() {
        return record.getHiredTime() != null ? DateTimeUtil.formatDateTime(record.getHiredTime()) : "-";
    }

    /**
     * Table column: last update time for the application.
     *
     * @return formatted update timestamp
     */
    public String getUpdatedTime() {
        return DateTimeUtil.formatDateTime(record.getUpdateTime());
    }

    /**
     * Indicates whether this application is in the hired state.
     * Used by table cell factories or action enablement, not as a table column.
     *
     * @return {@code true} if the record status is hired
     */
    public boolean isHired() {
        return record.isHired();
    }
}
