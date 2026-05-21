package com.bupt.tarecruit.viewmodel;

import com.bupt.tarecruit.entity.Job;
import com.bupt.tarecruit.entity.JobLog;
import com.bupt.tarecruit.util.DateTimeUtil;

/**
 * View model for presenting {@link JobLog} entries in administrator job audit tables.
 * Resolves job names from an optional related {@link Job} for readable table columns.
 */
public class JobLogDisplay {

    /** Underlying job log entity. */
    private final JobLog log;

    /** Related job entity used to resolve job name; may be {@code null}. */
    private final Job job;

    /**
     * Creates a display wrapper for a job log row.
     *
     * @param log job log entity; must not be {@code null}
     * @param job related job for name resolution; may be {@code null}
     */
    public JobLogDisplay(JobLog log, Job job) {
        this.log = log;
        this.job = job;
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
     * Table column: administrator who performed the action.
     *
     * @return admin user ID
     */
    public String getAdminId() {
        return log.getAdminId();
    }

    /**
     * Table column: type of job operation (display label).
     *
     * @return localized or formatted action label
     */
    public String getAction() {
        return log.getActionLabel();
    }

    /**
     * Table column: identifier of the job affected by the operation.
     * Maps to the job ID stored on the log record.
     *
     * @return target job ID
     */
    public String getTargetUserId() {
        return log.getJobId();
    }

    /**
     * Table column: human-readable job name.
     * Returns {@code "Unknown"} when the related job is not available.
     *
     * @return job name or fallback label
     */
    public String getJobName() {
        return job != null ? job.getJobName() : "Unknown";
    }

    /**
     * Table column: job or posting state before the change.
     *
     * @return previous state description, or empty if not applicable
     */
    public String getPreviousState() {
        return log.getPreviousState();
    }

    /**
     * Table column: job or posting state after the change.
     *
     * @return new state description, or empty if not applicable
     */
    public String getNewState() {
        return log.getNewState();
    }

    /**
     * Returns the wrapped job log entity for detail views or further processing.
     *
     * @return underlying {@link JobLog}
     */
    public JobLog getLog() {
        return log;
    }

    /**
     * Returns the related job entity used for name resolution.
     *
     * @return related {@link Job}, or {@code null} if not resolved
     */
    public Job getJob() {
        return job;
    }
}
