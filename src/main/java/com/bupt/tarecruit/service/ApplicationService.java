package com.bupt.tarecruit.service;

import com.bupt.tarecruit.dao.ApplicationDao;
import com.bupt.tarecruit.dao.JobDao;
import com.bupt.tarecruit.entity.ApplicationRecord;
import com.bupt.tarecruit.entity.ApplicationStatus;
import com.bupt.tarecruit.entity.Job;
import com.bupt.tarecruit.entity.JobStatus;
import com.bupt.tarecruit.util.IdGenerator;
import com.bupt.tarecruit.util.OperationResult;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ApplicationService {

    private final ApplicationDao applicationDao;
    private final JobDao jobDao;

    public ApplicationService(ApplicationDao applicationDao, JobDao jobDao) {
        this.applicationDao = applicationDao;
        this.jobDao = jobDao;
    }

    public List<ApplicationRecord> findByTa(String taId) {
        return applicationDao.findByTaId(taId).stream()
                .sorted(Comparator.comparing(ApplicationRecord::getUpdateTime).reversed())
                .collect(Collectors.toList());
    }

    /** TA application list (excludes withdrawn). */
    public List<ApplicationRecord> findActiveApplicationsForTa(String taId) {
        return applicationDao.findByTaId(taId).stream()
                .filter(r -> r.getStatus() != ApplicationStatus.WITHDRAWN)
                .sorted(Comparator.comparing(ApplicationRecord::getUpdateTime).reversed())
                .collect(Collectors.toList());
    }

    public List<ApplicationRecord> findByJob(String jobId) {
        return applicationDao.findByJobId(jobId).stream()
                .sorted(Comparator.comparing(ApplicationRecord::getUpdateTime).reversed())
                .collect(Collectors.toList());
    }

    /** Applicants for a job (excludes withdrawn). */
    public List<ApplicationRecord> findActiveApplicationsForJob(String jobId) {
        return applicationDao.findByJobId(jobId).stream()
                .filter(r -> r.getStatus() != ApplicationStatus.WITHDRAWN)
                .sorted(Comparator.comparing(ApplicationRecord::getUpdateTime).reversed())
                .collect(Collectors.toList());
    }

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
     * Count hired applicants for a specific job.
     */
    public int countHiredForJob(String jobId) {
        return applicationDao.findByJobId(jobId).stream()
                .filter(r -> r.getStatus() == ApplicationStatus.HIRED)
                .toList()
                .size();
    }

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

        // Check if job should be closed (when positions are filled)
        int hiredCount = countHiredForJob(job.getJobId());
        if (hiredCount >= job.getNumberOfPositions()) {
            job.setStatus(JobStatus.CLOSED);
            jobDao.update(job);

            // Reject remaining pending applications
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

    public OperationResult<Void> rejectApplicant(String applyId) {
        ApplicationRecord record = applicationDao.findById(applyId).orElse(null);
        if (record == null) {
            return OperationResult.failure("Application not found");
        }
        // If currently hired, clear hired time when rejecting
        if (record.getStatus() == ApplicationStatus.HIRED) {
            record.setHiredTime(null);
        }
        record.setStatus(ApplicationStatus.REJECTED);
        record.setUpdateTime(LocalDateTime.now());
        applicationDao.update(record);
        return OperationResult.success(null, "Marked as not hired");
    }

    /**
     * Undo reject - change status from REJECTED back to PENDING
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
     * When a job is closed, all still-pending applications for that job must be marked as rejected.
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
     * Startup data normalization: if a job is already closed in Jobs.csv but some applications are still Pending
     * in Applications.csv, fix them so users never see Pending for closed jobs.
     */
    public void normalizePendingApplicationsForClosedJobs() {
        List<Job> closedJobs = jobDao.findAll().stream()
                .filter(job -> !job.isOpen())
                .collect(Collectors.toList());
        for (Job job : closedJobs) {
            rejectPendingApplicationsForJob(job.getJobId());
        }
    }

    private String generateApplyId() {
        List<String> existing = applicationDao.findAll().stream()
                .map(ApplicationRecord::getApplyId)
                .collect(Collectors.toList());
        return IdGenerator.nextId("apply", existing);
    }
}
