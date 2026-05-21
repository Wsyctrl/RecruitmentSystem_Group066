package com.bupt.tarecruit.viewmodel;

import com.bupt.tarecruit.entity.ApplicationRecord;
import com.bupt.tarecruit.entity.ApplicationStatus;
import com.bupt.tarecruit.entity.Job;
import com.bupt.tarecruit.entity.Ta;
import com.bupt.tarecruit.util.DateTimeUtil;
import com.bupt.tarecruit.util.WorkloadRules;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * View model for presenting {@link Ta} records in the administrator teaching assistant table.
 * Aggregates application statistics, workload warnings, and nested job lists for detail panels.
 */
public class AdminTaDisplay {

    /** Underlying teaching assistant entity. */
    private final Ta ta;

    /** All application records for this TA. */
    private final List<ApplicationRecord> applications;

    /** Full job catalog used to resolve job details by ID. */
    private final List<Job> allJobs;

    /**
     * Creates a display wrapper for an administrator TA row.
     *
     * @param ta           teaching assistant entity; must not be {@code null}
     * @param applications this TA's application records; must not be {@code null}
     * @param allJobs      all jobs for ID lookup; must not be {@code null}
     */
    public AdminTaDisplay(Ta ta, List<ApplicationRecord> applications, List<Job> allJobs) {
        this.ta = ta;
        this.applications = applications;
        this.allJobs = allJobs;
    }

    /**
     * Table column: unique teaching assistant identifier.
     *
     * @return TA user ID
     */
    public String getTaId() {
        return ta.getTaId();
    }

    /**
     * Table column: full name of the teaching assistant.
     *
     * @return display full name
     */
    public String getFullName() {
        return ta.getFullName();
    }

    /**
     * Table column: contact phone number.
     *
     * @return phone number
     */
    public String getPhone() {
        return ta.getPhone();
    }

    /**
     * Table column: contact email address.
     *
     * @return email address
     */
    public String getEmail() {
        return ta.getEmail();
    }

    /**
     * Table column: account status (display label).
     *
     * @return localized or formatted status label
     */
    public String getStatusLabel() {
        return ta.getStatusLabel();
    }

    /**
     * Table column: number of active applications (excluding withdrawn).
     *
     * @return count of non-withdrawn applications
     */
    public int getAppliedCount() {
        return (int) applications.stream()
                .filter(a -> a.getStatus() != ApplicationStatus.WITHDRAWN)
                .count();
    }

    /**
     * Table column: number of applications in the hired state.
     *
     * @return count of hired applications
     */
    public int getHiredCount() {
        return (int) applications.stream()
                .filter(a -> a.getStatus() == ApplicationStatus.HIRED)
                .count();
    }

    /**
     * Table column: number of hired jobs currently within their start–end work period.
     *
     * @return count of ongoing hired assignments as of today
     */
    public int getCurrentOngoingJobsCount() {
        LocalDate now = LocalDate.now();
        return (int) applications.stream()
                .filter(a -> a.getStatus() == ApplicationStatus.HIRED)
                .map(a -> findJob(a.getJobId()))
                .filter(job -> job != null
                        && job.getStartDate() != null
                        && job.getEndDate() != null
                        && now.isAfter(job.getStartDate())
                        && now.isBefore(job.getEndDate()))
                .count();
    }

    /**
     * Indicates whether the TA exceeds the concurrent-job workload warning threshold.
     * Used by table cell factories or styling, not as a raw table column.
     *
     * @return {@code true} if {@link #getCurrentOngoingJobsCount()} exceeds
     *         {@link WorkloadRules#CONCURRENT_JOB_WARNING_THRESHOLD}
     */
    public boolean isOverConcurrentThreshold() {
        return getCurrentOngoingJobsCount() > WorkloadRules.CONCURRENT_JOB_WARNING_THRESHOLD;
    }

    /**
     * Factory method: builds display rows for all non-withdrawn applications.
     * Used by detail sub-tables or expansion panels.
     *
     * @return list of {@link JobApplicationInfo} for applied jobs
     */
    public List<JobApplicationInfo> getAppliedJobs() {
        return applications.stream()
                .filter(a -> a.getStatus() != ApplicationStatus.WITHDRAWN)
                .map(a -> {
                    Job job = findJob(a.getJobId());
                    return new JobApplicationInfo(job, a.getStatus(), a.getApplyTime());
                })
                .collect(Collectors.toList());
    }

    /**
     * Factory method: builds display rows for all hired applications.
     * Uses hire time when available for the time column.
     *
     * @return list of {@link JobApplicationInfo} for hired jobs
     */
    public List<JobApplicationInfo> getHiredJobs() {
        return applications.stream()
                .filter(a -> a.getStatus() == ApplicationStatus.HIRED)
                .map(a -> {
                    Job job = findJob(a.getJobId());
                    return new JobApplicationInfo(job, a.getStatus(), a.getHiredTime());
                })
                .collect(Collectors.toList());
    }

    private Job findJob(String jobId) {
        return allJobs.stream()
                .filter(j -> j.getJobId().equalsIgnoreCase(jobId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns the wrapped teaching assistant entity for detail views or actions.
     *
     * @return underlying {@link Ta}
     */
    public Ta getTa() {
        return ta;
    }

    /**
     * Nested view model for a single job row within TA application or hire detail tables.
     * Created by {@link #getAppliedJobs()} and {@link #getHiredJobs()}.
     */
    public static class JobApplicationInfo {

        /** Related job posting; may be {@code null} if not found in the catalog. */
        private final Job job;

        /** Application status at the time of display. */
        private final ApplicationStatus status;

        /** Relevant timestamp (apply time or hire time depending on factory source). */
        private final java.time.LocalDateTime time;

        /**
         * Creates a job-application display row.
         *
         * @param job    related job; may be {@code null}
         * @param status application status
         * @param time   apply or hire timestamp; may be {@code null}
         */
        public JobApplicationInfo(Job job, ApplicationStatus status, java.time.LocalDateTime time) {
            this.job = job;
            this.status = status;
            this.time = time;
        }

        /**
         * Table column: job title or posting name.
         *
         * @return job name, or {@code "Unknown"} if job is unavailable
         */
        public String getJobName() {
            return job != null ? job.getJobName() : "Unknown";
        }

        /**
         * Table column: unique job identifier.
         *
         * @return job ID, or {@code "Unknown"} if job is unavailable
         */
        public String getJobId() {
            return job != null ? job.getJobId() : "Unknown";
        }

        /**
         * Table column: apply or hire time formatted for display.
         *
         * @return formatted date-time string, or {@code "-"} if time is unavailable
         */
        public String getTime() {
            return time != null ? DateTimeUtil.formatDateTime(time) : "-";
        }

        /**
         * Table column: job work period from start date through end date.
         *
         * @return formatted period string {@code "start to end"}, or {@code "-"} if job is unavailable
         */
        public String getWorkPeriod() {
            if (job == null) {
                return "-";
            }
            String start = DateTimeUtil.formatDate(job.getStartDate());
            String end = DateTimeUtil.formatDate(job.getEndDate());
            return start + " to " + end;
        }

        /**
         * Returns the related job entity for navigation or actions.
         *
         * @return underlying {@link Job}, or {@code null} if not resolved
         */
        public Job getJob() {
            return job;
        }
    }
}
