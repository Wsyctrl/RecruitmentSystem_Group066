package com.bupt.tarecruit.entity;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Job posting created by a module organizer (MO) seeking teaching assistants.
 * Holds posting details, scheduling, keywords, and open/closed status.
 * Equality is based solely on {@link #jobId}.
 */
public class Job {

    /**
     * Unique identifier of this job posting.
     */
    private String jobId;

    /**
     * Title or short name of the job posting.
     */
    private String jobName;

    /**
     * Email-based identifier of the module organizer who owns this posting.
     */
    private String moId;

    /**
     * Display name of the module organizer for UI rendering; not persisted to CSV.
     */
    private String moName;

    /**
     * Number of teaching assistant positions available for this posting.
     */
    private int numberOfPositions;

    /**
     * Academic module or course associated with this posting.
     */
    private String moduleName;

    /**
     * Qualifications and skills required of applicants.
     */
    private String requirements;

    /**
     * Expected start date of the teaching assistant assignment.
     */
    private LocalDate startDate;

    /**
     * Expected end date of the teaching assistant assignment.
     */
    private LocalDate endDate;

    /**
     * Optional free-text notes supplementing the posting.
     */
    private String additionalNotes;

    /**
     * Comma-separated or free-form keywords used for search and matching.
     */
    private String keywords;

    /**
     * Whether the posting accepts applications; defaults to {@link JobStatus#OPEN}.
     */
    private JobStatus status = JobStatus.OPEN;

    /**
     * Returns the unique identifier of this job posting.
     *
     * @return job identifier
     */
    public String getJobId() {
        return jobId;
    }

    /**
     * Sets the unique identifier of this job posting.
     *
     * @param jobId job identifier
     */
    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    /**
     * Returns the title or short name of the job posting.
     *
     * @return job name
     */
    public String getJobName() {
        return jobName;
    }

    /**
     * Sets the title or short name of the job posting.
     *
     * @param jobName job name
     */
    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    /**
     * Returns the identifier of the module organizer who owns this posting.
     *
     * @return module organizer identifier
     */
    public String getMoId() {
        return moId;
    }

    /**
     * Sets the identifier of the module organizer who owns this posting.
     *
     * @param moId module organizer identifier
     */
    public void setMoId(String moId) {
        this.moId = moId;
    }

    /**
     * Returns the display name of the module organizer for UI rendering.
     *
     * @return module organizer display name, or {@code null} if not populated
     */
    public String getMoName() {
        return moName;
    }

    /**
     * Sets the display name of the module organizer for UI rendering.
     *
     * @param moName module organizer display name
     */
    public void setMoName(String moName) {
        this.moName = moName;
    }

    /**
     * Returns the number of teaching assistant positions available.
     *
     * @return position count
     */
    public int getNumberOfPositions() {
        return numberOfPositions;
    }

    /**
     * Sets the number of teaching assistant positions available.
     *
     * @param numberOfPositions position count
     */
    public void setNumberOfPositions(int numberOfPositions) {
        this.numberOfPositions = numberOfPositions;
    }

    /**
     * Returns the academic module or course associated with this posting.
     *
     * @return module name
     */
    public String getModuleName() {
        return moduleName;
    }

    /**
     * Sets the academic module or course associated with this posting.
     *
     * @param moduleName module name
     */
    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    /**
     * Returns the qualifications and skills required of applicants.
     *
     * @return requirements text
     */
    public String getRequirements() {
        return requirements;
    }

    /**
     * Sets the qualifications and skills required of applicants.
     *
     * @param requirements requirements text
     */
    public void setRequirements(String requirements) {
        this.requirements = requirements;
    }

    /**
     * Returns the expected start date of the assignment.
     *
     * @return start date
     */
    public LocalDate getStartDate() {
        return startDate;
    }

    /**
     * Sets the expected start date of the assignment.
     *
     * @param startDate start date
     */
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    /**
     * Returns the expected end date of the assignment.
     *
     * @return end date
     */
    public LocalDate getEndDate() {
        return endDate;
    }

    /**
     * Sets the expected end date of the assignment.
     *
     * @param endDate end date
     */
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    /**
     * Returns optional free-text notes for this posting.
     *
     * @return additional notes
     */
    public String getAdditionalNotes() {
        return additionalNotes;
    }

    /**
     * Sets optional free-text notes for this posting.
     *
     * @param additionalNotes additional notes
     */
    public void setAdditionalNotes(String additionalNotes) {
        this.additionalNotes = additionalNotes;
    }

    /**
     * Returns keywords used for search and matching.
     *
     * @return keywords text
     */
    public String getKeywords() {
        return keywords;
    }

    /**
     * Sets keywords used for search and matching.
     *
     * @param keywords keywords text
     */
    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    /**
     * Returns whether this posting is open or closed to applications.
     *
     * @return job status
     */
    public JobStatus getStatus() {
        return status;
    }

    /**
     * Sets whether this posting is open or closed to applications.
     *
     * @param status job status
     */
    public void setStatus(JobStatus status) {
        this.status = status;
    }

    /**
     * Returns a human-readable label for {@link #status}, suitable for UI display.
     *
     * @return display label of the job status
     */
    public String getStatusLabel() {
        return status.getLabel();
    }

    /**
     * Indicates whether this posting currently accepts applications.
     *
     * @return {@code true} if {@link #status} is {@link JobStatus#OPEN}
     */
    public boolean isOpen() {
        return status == JobStatus.OPEN;
    }

    /**
     * Compares this job to another object for equality.
     * Two jobs are equal when they share the same non-null {@link #jobId}.
     *
     * @param o object to compare
     * @return {@code true} if the other object is a {@link Job} with the same job identifier
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Job job)) return false;
        return Objects.equals(jobId, job.jobId);
    }

    /**
     * Returns a hash code based on {@link #jobId}.
     *
     * @return hash code consistent with {@link #equals(Object)}
     */
    @Override
    public int hashCode() {
        return Objects.hash(jobId);
    }
}
