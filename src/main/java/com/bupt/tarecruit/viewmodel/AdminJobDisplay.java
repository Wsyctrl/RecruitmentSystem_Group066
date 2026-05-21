package com.bupt.tarecruit.viewmodel;

import com.bupt.tarecruit.entity.Job;
import com.bupt.tarecruit.util.DateTimeUtil;

/**
 * View model for presenting {@link Job} records in the administrator job management table.
 * Combines job entity data with aggregate hire counts for table columns.
 */
public class AdminJobDisplay {

    /** Underlying job entity. */
    private final Job job;

    /** Number of teaching assistants currently hired for this job. */
    private final int hiredCount;

    /**
     * Creates a display wrapper for an administrator job row.
     *
     * @param job        job entity; must not be {@code null}
     * @param hiredCount number of hired applicants for this job
     */
    public AdminJobDisplay(Job job, int hiredCount) {
        this.job = job;
        this.hiredCount = hiredCount;
    }

    /**
     * Table column: unique job identifier.
     *
     * @return job ID
     */
    public String getJobId() {
        return job.getJobId();
    }

    /**
     * Table column: job title or posting name.
     *
     * @return job name
     */
    public String getJobName() {
        return job.getJobName();
    }

    /**
     * Table column: module organizer identifier.
     *
     * @return MO user ID
     */
    public String getMoId() {
        return job.getMoId();
    }

    /**
     * Table column: module organizer display name.
     * Returns {@code "-"} when the name is missing or blank.
     *
     * @return MO name or placeholder
     */
    public String getMoName() {
        if (job.getMoName() == null || job.getMoName().isBlank()) {
            return "-";
        }
        return job.getMoName();
    }

    /**
     * Table column: academic module associated with the job.
     *
     * @return module name
     */
    public String getModuleName() {
        return job.getModuleName();
    }

    /**
     * Table column: total positions available for hire.
     *
     * @return number of positions
     */
    public int getNumberOfPositions() {
        return job.getNumberOfPositions();
    }

    /**
     * Table column: number of positions already filled.
     *
     * @return hired applicant count
     */
    public int getHiredCount() {
        return hiredCount;
    }

    /**
     * Table column: job start date formatted for display.
     *
     * @return formatted start date string
     */
    public String getStartDateLabel() {
        return DateTimeUtil.formatDate(job.getStartDate());
    }

    /**
     * Table column: job end date formatted for display.
     *
     * @return formatted end date string
     */
    public String getEndDateLabel() {
        return DateTimeUtil.formatDate(job.getEndDate());
    }

    /**
     * Table column: job requirements or description text.
     *
     * @return requirements text
     */
    public String getRequirements() {
        return job.getRequirements();
    }

    /**
     * Table column: current job status (display label).
     *
     * @return localized or formatted status label
     */
    public String getStatusLabel() {
        return job.getStatusLabel();
    }

    /**
     * Returns the wrapped job entity for detail views or actions.
     *
     * @return underlying {@link Job}
     */
    public Job getJob() {
        return job;
    }
}
