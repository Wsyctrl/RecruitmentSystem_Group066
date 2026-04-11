package com.bupt.tarecruit.service;

import com.bupt.tarecruit.dao.JobDao;
import com.bupt.tarecruit.entity.Job;
import com.bupt.tarecruit.entity.JobStatus;
import com.bupt.tarecruit.util.IdGenerator;
import com.bupt.tarecruit.util.OperationResult;
import com.bupt.tarecruit.util.ValidationUtil;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Provides business logic for job publishing, job lookup,
 * job searching, and job status management.
 */
public class JobService {

    /**
     * Data access object for job records.
     */
    private final JobDao jobDao;

    /**
     * Creates a job service with the required data access dependency.
     *
     * @param jobDao data access object for job records
     */
    public JobService(JobDao jobDao) {
        this.jobDao = jobDao;
    }

    /**
     * Returns all open jobs sorted by start date.
     *
     * @return list of open jobs
     */
    public List<Job> findOpenJobs() {
        return jobDao.findAll().stream()
                .filter(Job::isOpen)
                .sorted(Comparator.comparing(Job::getStartDate, Comparator.nullsLast(LocalDate::compareTo)))
                .collect(Collectors.toList());
    }

    /**
     * Returns all jobs regardless of status.
     *
     * @return list of all jobs
     */
    public List<Job> findAllJobs() {
        return jobDao.findAll();
    }

    /**
     * Searches open jobs by job title, module name, or requirements text.
     *
     * @param keyword search keyword entered by the user
     * @return list of matching open jobs
     */
    public List<Job> searchOpenJobs(String keyword) {
        String lowerKeyword = keyword == null ? "" : keyword.toLowerCase();
        return findOpenJobs().stream()
                .filter(job -> contains(job.getJobName(), lowerKeyword)
                        || contains(job.getModuleName(), lowerKeyword)
                        || contains(job.getRequirements(), lowerKeyword))
                .collect(Collectors.toList());
    }

    /**
     * Checks whether the source text contains the given keyword.
     *
     * @param source source text
     * @param keyword search keyword
     * @return true if the keyword is contained in the source text; false otherwise
     */
    private boolean contains(String source, String keyword) {
        if (source == null) {
            return false;
        }
        return source.toLowerCase().contains(keyword);
    }

    /**
     * Returns all jobs created by a specific MO user, including closed jobs.
     *
     * @param moId MO user identifier
     * @return list of jobs posted by the specified MO user
     */
    public List<Job> findJobsByMo(String moId) {
        return jobDao.findAll().stream()
                .filter(job -> job.getMoId().equalsIgnoreCase(moId))
                .sorted(Comparator.comparing(Job::getJobId))
                .collect(Collectors.toList());
    }

    /**
     * Finds a job by its identifier.
     *
     * @param jobId job identifier
     * @return optional containing the job if found
     */
    public Optional<Job> findById(String jobId) {
        return jobDao.findById(jobId);
    }

    /**
     * Creates a new job or updates an existing one after validating required fields.
     *
     * @param job job entity to create or update
     * @return operation result containing the saved job
     * @throws IllegalArgumentException if required fields are missing or the date range is invalid
     */
    public OperationResult<Job> upsertJob(Job job) {
        boolean missingRequired = ValidationUtil.isBlank(job.getJobName())
                || ValidationUtil.isBlank(job.getModuleName())
                || job.getNumberOfPositions() <= 0
                || job.getStartDate() == null
                || job.getEndDate() == null;
        if (missingRequired) {
            throw new IllegalArgumentException("Please fill all required fields before publishing (Job title, Module, Positions, Start date, End date).");
        }
        ValidationUtil.requireNonBlank(job.getJobName(), "Job title is required");
        ValidationUtil.requireNonBlank(job.getModuleName(), "Module is required");
        if (job.getStartDate() != null && job.getEndDate() != null && job.getEndDate().isBefore(job.getStartDate())) {
            throw new IllegalArgumentException("End date must not be earlier than start date.");
        }
        ValidationUtil.requireNonBlank(job.getMoId(), "MO account is missing");
        if (job.getJobId() == null || job.getJobId().isBlank()) {
            job.setJobId(generateJobId());
            job.setStatus(JobStatus.OPEN);
            jobDao.save(job);
            return OperationResult.success(job, "Job posted");
        } else {
            jobDao.update(job);
            return OperationResult.success(job, "Job updated");
        }
    }

    /**
     * Closes a job by changing its status to CLOSED.
     *
     * @param jobId target job identifier
     * @return operation result describing whether the close action succeeded
     */
    public OperationResult<Void> closeJob(String jobId) {
        Job job = jobDao.findById(jobId).orElse(null);
        if (job == null) {
            return OperationResult.failure("Job not found");
        }
        job.setStatus(JobStatus.CLOSED);
        jobDao.update(job);
        return OperationResult.success(null, "Job closed");
    }

    /**
     * Reopens a job by changing its status to OPEN.
     *
     * @param jobId target job identifier
     * @return operation result describing whether the reopen action succeeded
     */
    public OperationResult<Void> openJob(String jobId) {
        Job job = jobDao.findById(jobId).orElse(null);
        if (job == null) {
            return OperationResult.failure("Job not found");
        }
        job.setStatus(JobStatus.OPEN);
        jobDao.update(job);
        return OperationResult.success(null, "Job reopened");
    }

    /**
     * Generates the next unique job identifier.
     *
     * @return generated job identifier
     */
    private String generateJobId() {
        List<String> existing = jobDao.findAll().stream().map(Job::getJobId).collect(Collectors.toList());
        return IdGenerator.nextId("job", existing);
    }
}