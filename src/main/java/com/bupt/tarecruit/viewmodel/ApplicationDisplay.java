package com.bupt.tarecruit.viewmodel;

import com.bupt.tarecruit.entity.ApplicationRecord;
import com.bupt.tarecruit.entity.Job;
import com.bupt.tarecruit.util.DateTimeUtil;

/**
 * View model for presenting a TA's {@link ApplicationRecord} together with its {@link Job}
 * in application history or status tables.
 */
public class ApplicationDisplay {

    /** Underlying application record. */
    private final ApplicationRecord record;

    /** Job associated with the application; may be {@code null} if unresolved. */
    private final Job job;

    /**
     * Creates a display wrapper for an application row.
     *
     * @param record application record; must not be {@code null}
     * @param job    related job posting; may be {@code null}
     */
    public ApplicationDisplay(ApplicationRecord record, Job job) {
        this.record = record;
        this.job = job;
    }

    /**
     * Returns the wrapped application record for actions or detail views.
     *
     * @return underlying {@link ApplicationRecord}
     */
    public ApplicationRecord getRecord() {
        return record;
    }

    /**
     * Returns the related job entity.
     *
     * @return underlying {@link Job}, or {@code null} if not available
     */
    public Job getJob() {
        return job;
    }

    /**
     * Table column: job title or posting name.
     *
     * @return job name, or empty string if job is unavailable
     */
    public String getJobName() {
        return job != null ? job.getJobName() : "";
    }

    /**
     * Table column: unique application identifier.
     *
     * @return apply ID
     */
    public String getApplyId() {
        return record.getApplyId();
    }

    /**
     * Table column: academic module for the job.
     *
     * @return module name, or empty string if job is unavailable
     */
    public String getModuleName() {
        return job != null ? job.getModuleName() : "";
    }

    /**
     * Table column: current application status (display label).
     *
     * @return localized or formatted status label
     */
    public String getStatusLabel() {
        return record.getStatus().getLabel();
    }

    /**
     * Table column: last update time formatted for display.
     *
     * @return formatted update timestamp
     */
    public String getUpdatedTime() {
        return DateTimeUtil.formatDateTime(record.getUpdateTime());
    }

    /**
     * Table column: job start date formatted for display.
     *
     * @return formatted start date, or {@code "-"} if job is unavailable
     */
    public String getJobStartDate() {
        return job != null ? DateTimeUtil.formatDate(job.getStartDate()) : "-";
    }

    /**
     * Table column: job end date formatted for display.
     *
     * @return formatted end date, or {@code "-"} if job is unavailable
     */
    public String getJobEndDate() {
        return job != null ? DateTimeUtil.formatDate(job.getEndDate()) : "-";
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
     * Table column: combined job work period (start through end).
     *
     * @return formatted period string {@code "start - end"}, or {@code "-"} if job is unavailable
     */
    public String getJobPeriod() {
        if (job == null) {
            return "-";
        }
        String start = DateTimeUtil.formatDate(job.getStartDate());
        String end = DateTimeUtil.formatDate(job.getEndDate());
        return start + " - " + end;
    }
}
