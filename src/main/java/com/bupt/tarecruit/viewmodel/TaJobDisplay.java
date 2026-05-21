package com.bupt.tarecruit.viewmodel;

import com.bupt.tarecruit.entity.Job;
import com.bupt.tarecruit.util.DateTimeUtil;

/**
 * View model for presenting {@link Job} postings in the teaching assistant job browse table.
 * Includes application and hire counts for capacity and competition at a glance.
 */
public class TaJobDisplay {

    /** Underlying job entity. */
    private final Job job;

    /** Total number of applicants for this job (non-withdrawn). */
    private final int applicantsCount;

    /** Number of applicants hired for this job. */
    private final int hiredCount;

    /**
     * Creates a display wrapper for a TA-facing job row.
     *
     * @param job             job entity; must not be {@code null}
     * @param applicantsCount number of applicants
     * @param hiredCount      number of hired teaching assistants
     */
    public TaJobDisplay(Job job, int applicantsCount, int hiredCount) {
        this.job = job;
        this.applicantsCount = applicantsCount;
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
     * Table column: academic module associated with the job.
     *
     * @return module name
     */
    public String getModuleName() {
        return job.getModuleName();
    }

    /**
     * Table column: module organizer display name.
     *
     * @return MO name
     */
    public String getMoName() {
        return job.getMoName();
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
     * Table column: date when the job was posted (uses job start date).
     *
     * @return formatted posted date string
     */
    public String getPostedDate() {
        // Start date is used as posted date
        return DateTimeUtil.formatDate(job.getStartDate());
    }

    /**
     * Table column: number of applicants for this job.
     *
     * @return applicant count
     */
    public int getApplicantsCount() {
        return applicantsCount;
    }

    /**
     * Table column: number of positions already filled.
     *
     * @return hired count
     */
    public int getHiredCount() {
        return hiredCount;
    }

    /**
     * Returns the wrapped job entity for apply actions or detail views.
     *
     * @return underlying {@link Job}
     */
    public Job getJob() {
        return job;
    }
}
