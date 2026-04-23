package com.bupt.tarecruit.service;

import com.bupt.tarecruit.dao.ApplicationDao;
import com.bupt.tarecruit.dao.JobDao;
import com.bupt.tarecruit.entity.ApplicationRecord;
import com.bupt.tarecruit.entity.ApplicationStatus;
import com.bupt.tarecruit.entity.Job;
import com.bupt.tarecruit.entity.JobStatus;
import com.bupt.tarecruit.util.IdGenerator;
import com.bupt.tarecruit.util.OperationResult;
import com.bupt.tarecruit.util.WorkloadRules;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Provides business logic for TA job applications, including submission,
 * withdrawal, hiring, rejection, and application status normalization.
 */
public class ApplicationService {

    /**
     * Data access object for application records.
     */
    private final ApplicationDao applicationDao;

    /**
     * Data access object for job records.
     */
    private final JobDao jobDao;

    /**
     * Creates an application service with required data access dependencies.
     *
     * @param applicationDao data access object for application records
     * @param jobDao data access object for job records
     */
    public ApplicationService(ApplicationDao applicationDao, JobDao jobDao) {
        this.applicationDao = applicationDao;
        this.jobDao = jobDao;
    }

    /**
     * Returns all applications submitted by a specific TA user,
     * sorted by latest update time in descending order.
     *
     * @param taId TA user identifier
     * @return list of applications submitted by the TA user
     */
    public List<ApplicationRecord> findByTa(String taId) {
        return applicationDao.findByTaId(taId).stream()
                .sorted(Comparator.comparing(ApplicationRecord::getUpdateTime).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Returns all active applications submitted by a specific TA user,
     * excluding withdrawn records.
     *
     * @param taId TA user identifier
     * @return list of active applications for the TA user
     */
    public List<ApplicationRecord> findActiveApplicationsForTa(String taId) {
        return applicationDao.findByTaId(taId).stream()
                .filter(r -> r.getStatus() != ApplicationStatus.WITHDRAWN)
                .sorted(Comparator.comparing(ApplicationRecord::getUpdateTime).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Returns all application records for a specific job,
     * sorted by latest update time in descending order.
     *
     * @param jobId target job identifier
     * @return list of applications for the job
     */
    public List<ApplicationRecord> findByJob(String jobId) {
        return applicationDao.findByJobId(jobId).stream()
                .sorted(Comparator.comparing(ApplicationRecord::getUpdateTime).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Returns all active applicants for a specific job,
     * excluding withdrawn application records.
     *
     * @param jobId target job identifier
     * @return list of active applications for the job
     */
    public List<ApplicationRecord> findActiveApplicationsForJob(String jobId) {
        return applicationDao.findByJobId(jobId).stream()
                .filter(r -> r.getStatus() != ApplicationStatus.WITHDRAWN)
                .sorted(Comparator.comparing(ApplicationRecord::getUpdateTime).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Submits a new application for a TA user to an open job.
     * Rejects duplicate active applications for the same job.
     *
     * @param taId TA user identifier
     * @param job target job
     * @return operation result containing the created application record on success
     */
    public OperationResult<ApplicationRecord> applyForJob(String taId, Job job) {
        if (job == null || !job.isOpen()) {
            return OperationResult.failure("This job is not open for applications");
        }
        boolean alreadyApplied = applicationDao.findByTaId(taId).stream()
                .anyMatch(record -> record.getJobId().equalsIgnoreCase(job.getJobId())
                        && record.getStatus() != ApplicationStatus.WITHDRAWN);
        if (alreadyApplied) {
            return OperationResult.failure("You already have an active application for this job");
        }
        ApplicationRecord record = new ApplicationRecord();
        record.setApplyId(generateApplyId());
        record.setJobId(job.getJobId());
        record.setTaId(taId);
        record.setStatus(ApplicationStatus.PENDING);
        record.setUpdateTime(LocalDateTime.now());
        applicationDao.save(record);
        return OperationResult.success(record, "Application submitted");
    }

    /**
     * Withdraws a pending application belonging to the specified TA user.
     *
     * @param applyId application identifier
     * @param taId TA user identifier
     * @return operation result describing whether the withdrawal succeeded
     */
    public OperationResult<Void> withdraw(String applyId, String taId) {
        Optional<ApplicationRecord> recordOpt = applicationDao.findById(applyId);
        if (recordOpt.isEmpty()) {
            return OperationResult.failure("Application not found");
        }
        ApplicationRecord record = recordOpt.get();
        if (!record.getTaId().equalsIgnoreCase(taId)) {
            return OperationResult.failure("You cannot withdraw another user's application");
        }
        if (!record.isPending()) {
            return OperationResult.failure("Only pending applications can be withdrawn");
        }
        record.setStatus(ApplicationStatus.WITHDRAWN);
        record.setUpdateTime(LocalDateTime.now());
        applicationDao.update(record);
        return OperationResult.success(null, "Application withdrawn");
    }

    /**
     * Counts the number of hired applicants for a specific job.
     *
     * @param jobId target job identifier
     * @return number of hired applicants for the job
     */
    public int countHiredForJob(String jobId) {
        return applicationDao.findByJobId(jobId).stream()
                .filter(r -> r.getStatus() == ApplicationStatus.HIRED)
                .toList()
                .size();
    }

    /**
     * Counts hired jobs that are currently ongoing for a TA.
     *
     * @param taId TA user identifier
     * @param now current date used for time-window checks
     * @return number of currently ongoing hired jobs
     */
    public int countCurrentOngoingHiredJobs(String taId, LocalDate now) {
        return findCurrentOngoingHiredJobs(taId, now).size();
    }

    /**
     * Finds hired jobs currently in progress for a TA.
     *
     * @param taId TA user identifier
     * @param now current date used for time-window checks
     * @return currently ongoing hired jobs
     */
    public List<Job> findCurrentOngoingHiredJobs(String taId, LocalDate now) {
        return applicationDao.findByTaId(taId).stream()
                .filter(record -> record.getStatus() == ApplicationStatus.HIRED)
                .map(record -> jobDao.findById(record.getJobId()).orElse(null))
                .filter(job -> job != null && isDateWithinJobRange(now, job))
                .collect(Collectors.toList());
    }

    /**
     * Finds existing hired jobs that overlap with the target job period.
     *
     * @param taId TA user identifier
     * @param targetJobId target job identifier
     * @return overlapping hired jobs, excluding the target job itself
     */
    public List<Job> findOverlappingHiredJobs(String taId, String targetJobId) {
        Job targetJob = jobDao.findById(targetJobId).orElse(null);
        if (targetJob == null) {
            return List.of();
        }
        return applicationDao.findByTaId(taId).stream()
                .filter(record -> record.getStatus() == ApplicationStatus.HIRED)
                .map(record -> jobDao.findById(record.getJobId()).orElse(null))
                .filter(job -> job != null && !job.getJobId().equalsIgnoreCase(targetJobId))
                .filter(job -> isOverlapping(job, targetJob))
                .collect(Collectors.toList());
    }

    /**
     * Returns whether hiring should show a concurrent-jobs warning.
     *
     * @param taId TA user identifier
     * @param targetJobId target job identifier
     * @return true when overlap count reaches the warning threshold
     */
    public boolean shouldWarnConcurrentHire(String taId, String targetJobId) {
        return findOverlappingHiredJobs(taId, targetJobId).size() >= WorkloadRules.CONCURRENT_JOB_WARNING_THRESHOLD;
    }

    /**
     * Marks an application as hired. If all job positions become filled,
     * the job is closed and remaining pending applications are rejected.
     *
     * @param applyId application identifier
     * @return operation result describing whether the hire action succeeded
     */
    public OperationResult<Void> hireApplicant(String applyId) {
        ApplicationRecord record = applicationDao.findById(applyId).orElse(null);
        if (record == null) {
            return OperationResult.failure("Application not found");
        }
        Job job = jobDao.findById(record.getJobId()).orElse(null);
        if (job == null) {
            return OperationResult.failure("Job not found");
        }
        record.setStatus(ApplicationStatus.HIRED);
        record.setUpdateTime(LocalDateTime.now());
        record.setHiredTime(LocalDateTime.now());
        applicationDao.update(record);

        int hiredCount = countHiredForJob(job.getJobId());
        if (hiredCount >= job.getNumberOfPositions()) {
            job.setStatus(JobStatus.CLOSED);
            jobDao.update(job);

            List<ApplicationRecord> others = applicationDao.findByJobId(job.getJobId());
            for (ApplicationRecord other : others) {
                if (other.getStatus() == ApplicationStatus.PENDING) {
                    other.setStatus(ApplicationStatus.REJECTED);
                    other.setUpdateTime(LocalDateTime.now());
                    applicationDao.update(other);
                }
            }
            return OperationResult.success(null, "Applicant hired; job closed (position filled)");
        }

        return OperationResult.success(null, "Applicant hired (" + hiredCount + "/" + job.getNumberOfPositions() + " positions filled)");
    }

    /**
     * Restores a hired applicant back to pending status.
     *
     * @param applyId application identifier
     * @return operation result describing whether the status change succeeded
     */
    public OperationResult<Void> unhireApplicant(String applyId) {
        ApplicationRecord record = applicationDao.findById(applyId).orElse(null);
        if (record == null) {
            return OperationResult.failure("Application not found");
        }
        if (record.getStatus() != ApplicationStatus.HIRED) {
            return OperationResult.failure("Only hired applicants can be unhired");
        }
        record.setStatus(ApplicationStatus.PENDING);
        record.setUpdateTime(LocalDateTime.now());
        record.setHiredTime(null);
        applicationDao.update(record);
        return OperationResult.success(null, "Applicant status changed back to Pending");
    }

    /**
     * Marks an application as rejected. If the application was previously hired,
     * the hired timestamp is cleared.
     *
     * @param applyId application identifier
     * @return operation result describing whether the reject action succeeded
     */
    public OperationResult<Void> rejectApplicant(String applyId) {
        ApplicationRecord record = applicationDao.findById(applyId).orElse(null);
        if (record == null) {
            return OperationResult.failure("Application not found");
        }
        if (record.getStatus() == ApplicationStatus.HIRED) {
            record.setHiredTime(null);
        }
        record.setStatus(ApplicationStatus.REJECTED);
        record.setUpdateTime(LocalDateTime.now());
        applicationDao.update(record);
        return OperationResult.success(null, "Marked as not hired");
    }

    /**
     * Restores a rejected application back to pending status.
     *
     * @param applyId application identifier
     * @return operation result describing whether the status change succeeded
     */
    public OperationResult<Void> unrejectApplicant(String applyId) {
        ApplicationRecord record = applicationDao.findById(applyId).orElse(null);
        if (record == null) {
            return OperationResult.failure("Application not found");
        }
        if (record.getStatus() != ApplicationStatus.REJECTED) {
            return OperationResult.failure("Only rejected applications can be unrejected");
        }
        record.setStatus(ApplicationStatus.PENDING);
        record.setUpdateTime(LocalDateTime.now());
        applicationDao.update(record);
        return OperationResult.success(null, "Application status changed back to Pending");
    }

    /**
     * Rejects all still-pending applications for a specific job.
     * This is typically used when a job is manually closed.
     *
     * @param jobId target job identifier
     * @return operation result describing how many records were updated
     */
    public OperationResult<Void> rejectPendingApplicationsForJob(String jobId) {
        List<ApplicationRecord> records = applicationDao.findByJobId(jobId);
        int updated = 0;
        for (ApplicationRecord record : records) {
            if (record.getStatus() == ApplicationStatus.PENDING) {
                record.setStatus(ApplicationStatus.REJECTED);
                record.setUpdateTime(LocalDateTime.now());
                applicationDao.update(record);
                updated++;
            }
        }
        if (updated == 0) {
            return OperationResult.success(null, "No pending applications to reject");
        }
        return OperationResult.success(null, "Pending applications marked as not hired");
    }

    /**
     * Normalizes startup data by converting pending applications
     * for already-closed jobs into rejected status.
     */
    public void normalizePendingApplicationsForClosedJobs() {
        List<Job> closedJobs = jobDao.findAll().stream()
                .filter(job -> !job.isOpen())
                .collect(Collectors.toList());
        for (Job job : closedJobs) {
            rejectPendingApplicationsForJob(job.getJobId());
        }
    }

    /**
     * Generates the next unique application identifier.
     *
     * @return generated application identifier
     */
    private String generateApplyId() {
        List<String> existing = applicationDao.findAll().stream()
                .map(ApplicationRecord::getApplyId)
                .collect(Collectors.toList());
        return IdGenerator.nextId("apply", existing);
    }

    private boolean isDateWithinJobRange(LocalDate date, Job job) {
        if (date == null || job.getStartDate() == null || job.getEndDate() == null) {
            return false;
        }
        return date.isAfter(job.getStartDate()) && date.isBefore(job.getEndDate());
    }

    private boolean isOverlapping(Job existingJob, Job targetJob) {
        if (existingJob.getStartDate() == null || existingJob.getEndDate() == null
                || targetJob.getStartDate() == null || targetJob.getEndDate() == null) {
            return false;
        }
        LocalDate existingStart = existingJob.getStartDate();
        LocalDate existingEnd = existingJob.getEndDate();
        LocalDate targetStart = targetJob.getStartDate();
        LocalDate targetEnd = targetJob.getEndDate();
        return existingStart.isBefore(targetEnd) && existingEnd.isAfter(targetStart);
    }
}