package com.bupt.tarecruit.entity;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a teaching assistant's application to a specific job posting.
 * Tracks lifecycle status, application timestamps, and optional hire time.
 * Equality is based solely on {@link #applyId}.
 */
public class ApplicationRecord {

    /**
     * Unique identifier of this application record.
     */
    private String applyId;

    /**
     * Identifier of the teaching assistant who submitted the application.
     */
    private String taId;

    /**
     * Identifier of the job posting being applied to.
     */
    private String jobId;

    /**
     * Current status of the application; defaults to {@link ApplicationStatus#PENDING}.
     */
    private ApplicationStatus status = ApplicationStatus.PENDING;

    /**
     * Date and time when the application record was last updated.
     */
    private LocalDateTime updateTime = LocalDateTime.now();

    /**
     * Date and time when the application was originally submitted.
     */
    private LocalDateTime applyTime = LocalDateTime.now();

    /**
     * Date and time when the applicant was hired; {@code null} until hired.
     */
    private LocalDateTime hiredTime;

    /**
     * Returns the unique identifier of this application record.
     *
     * @return application identifier
     */
    public String getApplyId() {
        return applyId;
    }

    /**
     * Sets the unique identifier of this application record.
     *
     * @param applyId application identifier
     */
    public void setApplyId(String applyId) {
        this.applyId = applyId;
    }

    /**
     * Returns the identifier of the teaching assistant who submitted the application.
     *
     * @return teaching assistant identifier
     */
    public String getTaId() {
        return taId;
    }

    /**
     * Sets the identifier of the teaching assistant who submitted the application.
     *
     * @param taId teaching assistant identifier
     */
    public void setTaId(String taId) {
        this.taId = taId;
    }

    /**
     * Returns the identifier of the job posting being applied to.
     *
     * @return job identifier
     */
    public String getJobId() {
        return jobId;
    }

    /**
     * Sets the identifier of the job posting being applied to.
     *
     * @param jobId job identifier
     */
    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    /**
     * Returns the current status of the application.
     *
     * @return application status
     */
    public ApplicationStatus getStatus() {
        return status;
    }

    /**
     * Sets the current status of the application.
     *
     * @param status application status
     */
    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    /**
     * Returns the date and time when the application record was last updated.
     *
     * @return last update timestamp
     */
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    /**
     * Sets the date and time when the application record was last updated.
     *
     * @param updateTime last update timestamp
     */
    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    /**
     * Returns the date and time when the application was originally submitted.
     *
     * @return application submission timestamp
     */
    public LocalDateTime getApplyTime() {
        return applyTime;
    }

    /**
     * Sets the date and time when the application was originally submitted.
     *
     * @param applyTime application submission timestamp
     */
    public void setApplyTime(LocalDateTime applyTime) {
        this.applyTime = applyTime;
    }

    /**
     * Returns the date and time when the applicant was hired.
     *
     * @return hire timestamp, or {@code null} if not yet hired
     */
    public LocalDateTime getHiredTime() {
        return hiredTime;
    }

    /**
     * Sets the date and time when the applicant was hired.
     *
     * @param hiredTime hire timestamp
     */
    public void setHiredTime(LocalDateTime hiredTime) {
        this.hiredTime = hiredTime;
    }

    /**
     * Indicates whether this application is still awaiting a decision.
     *
     * @return {@code true} if {@link #status} is {@link ApplicationStatus#PENDING}
     */
    public boolean isPending() {
        return status == ApplicationStatus.PENDING;
    }

    /**
     * Indicates whether this application has been accepted and the applicant hired.
     *
     * @return {@code true} if {@link #status} is {@link ApplicationStatus#HIRED}
     */
    public boolean isHired() {
        return status == ApplicationStatus.HIRED;
    }

    /**
     * Compares this application to another object for equality.
     * Two records are equal when they share the same non-null {@link #applyId}.
     *
     * @param o object to compare
     * @return {@code true} if the other object is an {@link ApplicationRecord}
     *         with the same apply identifier
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ApplicationRecord)) return false;
        ApplicationRecord that = (ApplicationRecord) o;
        return Objects.equals(applyId, that.applyId);
    }

    /**
     * Returns a hash code based on {@link #applyId}.
     *
     * @return hash code consistent with {@link #equals(Object)}
     */
    @Override
    public int hashCode() {
        return Objects.hash(applyId);
    }
}
