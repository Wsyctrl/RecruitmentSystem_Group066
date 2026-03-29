package com.bupt.tarecruit.service;

import com.bupt.tarecruit.dao.JobDao;
import com.bupt.tarecruit.dao.MoDao;
import com.bupt.tarecruit.dao.TaDao;
import com.bupt.tarecruit.entity.Job;
import com.bupt.tarecruit.entity.JobStatus;
import com.bupt.tarecruit.entity.Mo;
import com.bupt.tarecruit.entity.Role;
import com.bupt.tarecruit.entity.Ta;
import com.bupt.tarecruit.util.OperationResult;

import java.util.List;

public class AdminService {

    private static final String DEFAULT_PASSWORD = "Pass@123";

    private final TaDao taDao;
    private final MoDao moDao;
    private final JobDao jobDao;

    public AdminService(TaDao taDao, MoDao moDao, JobDao jobDao) {
        this.taDao = taDao;
        this.moDao = moDao;
        this.jobDao = jobDao;
    }

    public List<Ta> findAllTa() {
        return taDao.findAll();
    }

    public List<Mo> findAllMo() {
        return moDao.findAll();
    }

    public OperationResult<Void> resetPassword(Role role, String userId) {
        switch (role) {
            case TA -> {
                Ta ta = taDao.findById(userId).orElse(null);
                if (ta == null) return OperationResult.failure("TA account not found");
                ta.setPassword(DEFAULT_PASSWORD);
                taDao.update(ta);
            }
            case MO, ADMIN -> {
                Mo mo = moDao.findById(userId).orElse(null);
                if (mo == null) return OperationResult.failure("MO account not found");
                mo.setPassword(DEFAULT_PASSWORD);
                moDao.update(mo);
            }
            default -> {
                return OperationResult.failure("Unknown role");
            }
        }
        return OperationResult.success(null, "Password reset to " + DEFAULT_PASSWORD);
    }

    public OperationResult<Void> toggleStatus(Role role, String userId, boolean disabled) {
        switch (role) {
            case TA -> {
                Ta ta = taDao.findById(userId).orElse(null);
                if (ta == null) return OperationResult.failure("TA account not found");
                ta.setDisabled(disabled);
                taDao.update(ta);
            }
            case MO, ADMIN -> {
                Mo mo = moDao.findById(userId).orElse(null);
                if (mo == null) return OperationResult.failure("MO account not found");
                mo.setDisabled(disabled);
                moDao.update(mo);
            }
            default -> {
                return OperationResult.failure("Unknown role");
            }
        }
        return OperationResult.success(null, disabled ? "Account disabled" : "Account enabled");
    }

    public List<Job> findAllJobs() {
        return jobDao.findAll();
    }

    /** Admin permanently closes a job (admin global jobs tab). Reopening is not allowed. */
    public OperationResult<Void> toggleJobOpenClosed(String jobId) {
        Job job = jobDao.findById(jobId).orElse(null);
        if (job == null) {
            return OperationResult.failure("Job not found");
        }
        if (!job.isOpen()) {
            return OperationResult.failure("Job is already closed");
        }
        job.setStatus(JobStatus.CLOSED);
        jobDao.update(job);
        return OperationResult.success(null, "Job has been closed");
    }
}
