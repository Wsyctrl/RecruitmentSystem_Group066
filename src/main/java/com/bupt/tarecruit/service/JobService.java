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

public class JobService {

    private final JobDao jobDao;

    public JobService(JobDao jobDao) {
        this.jobDao = jobDao;
    }

    public List<Job> findOpenJobs() {
        return jobDao.findAll().stream()
                .filter(Job::isOpen)
                .sorted(Comparator.comparing(Job::getStartDate, Comparator.nullsLast(LocalDate::compareTo)))
                .collect(Collectors.toList());
    }

    public List<Job> findAllJobs() {
        return jobDao.findAll();
    }

    public List<Job> searchOpenJobs(String keyword) {
        String lowerKeyword = keyword == null ? "" : keyword.toLowerCase();
        return findOpenJobs().stream()
                .filter(job -> contains(job.getJobName(), lowerKeyword)
                        || contains(job.getModuleName(), lowerKeyword)
                        || contains(job.getRequirements(), lowerKeyword))
                .collect(Collectors.toList());
    }

    private boolean contains(String source, String keyword) {
        if (source == null) {
            return false;
        }
        return source.toLowerCase().contains(keyword);
    }

    /** MO dashboard: all jobs including closed ones. */
    public List<Job> findJobsByMo(String moId) {
        return jobDao.findAll().stream()
                .filter(job -> job.getMoId().equalsIgnoreCase(moId))
                .sorted(Comparator.comparing(Job::getJobId))
                .collect(Collectors.toList());
    }

    public Optional<Job> findById(String jobId) {
        return jobDao.findById(jobId);
    }

    public OperationResult<Job> upsertJob(Job job) {
        // Required fields: job name, module name, positions, start date, end date.
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

    public OperationResult<Void> closeJob(String jobId) {
        Job job = jobDao.findById(jobId).orElse(null);
        if (job == null) {
            return OperationResult.failure("Job not found");
        }
        job.setStatus(JobStatus.CLOSED);
        jobDao.update(job);
        return OperationResult.success(null, "Job closed");
    }

    public OperationResult<Void> openJob(String jobId) {
        Job job = jobDao.findById(jobId).orElse(null);
        if (job == null) {
            return OperationResult.failure("Job not found");
        }
        job.setStatus(JobStatus.OPEN);
        jobDao.update(job);
        return OperationResult.success(null, "Job reopened");
    }

    private String generateJobId() {
        List<String> existing = jobDao.findAll().stream().map(Job::getJobId).collect(Collectors.toList());
        return IdGenerator.nextId("job", existing);
    }
}
