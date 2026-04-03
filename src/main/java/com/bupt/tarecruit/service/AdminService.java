package com.bupt.tarecruit.service;

import com.bupt.tarecruit.dao.AccountLogDao;
import com.bupt.tarecruit.dao.JobDao;
import com.bupt.tarecruit.dao.MoDao;
import com.bupt.tarecruit.dao.TaDao;
import com.bupt.tarecruit.entity.AccountLog;
import com.bupt.tarecruit.entity.Job;
import com.bupt.tarecruit.entity.JobStatus;
import com.bupt.tarecruit.entity.Mo;
import com.bupt.tarecruit.entity.Role;
import com.bupt.tarecruit.entity.Ta;
import com.bupt.tarecruit.util.IdGenerator;
import com.bupt.tarecruit.util.OperationResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class AdminService {

    private static final String DEFAULT_PASSWORD = "Pass@123";

    private final TaDao taDao;
    private final MoDao moDao;
    private final JobDao jobDao;
    private final AccountLogDao accountLogDao;

    public AdminService(TaDao taDao, MoDao moDao, JobDao jobDao, AccountLogDao accountLogDao) {
        this.taDao = taDao;
        this.moDao = moDao;
        this.jobDao = jobDao;
        this.accountLogDao = accountLogDao;
    }

    public List<Ta> findAllTa() {
        return taDao.findAll();
    }

    public List<Mo> findAllMo() {
        return moDao.findAll();
    }

    public OperationResult<Void> resetPassword(Role role, String userId, String adminId) {
        switch (role) {
            case TA -> {
                Ta ta = taDao.findById(userId).orElse(null);
                if (ta == null) return OperationResult.failure("TA account not found");
                ta.setPassword(DEFAULT_PASSWORD);
                taDao.update(ta);
                logAccountAction(adminId, userId, role, AccountLog.AccountAction.RESET_PASSWORD, "Password changed", "Password: " + DEFAULT_PASSWORD);
            }
            case MO, ADMIN -> {
                Mo mo = moDao.findById(userId).orElse(null);
                if (mo == null) return OperationResult.failure("MO account not found");
                mo.setPassword(DEFAULT_PASSWORD);
                moDao.update(mo);
                logAccountAction(adminId, userId, role, AccountLog.AccountAction.RESET_PASSWORD, "Password changed", "Password: " + DEFAULT_PASSWORD);
            }
            default -> {
                return OperationResult.failure("Unknown role");
            }
        }
        return OperationResult.success(null, "Password reset to " + DEFAULT_PASSWORD);
    }

    public OperationResult<Void> toggleStatus(Role role, String userId, boolean disabled, String adminId) {
        switch (role) {
            case TA -> {
                Ta ta = taDao.findById(userId).orElse(null);
                if (ta == null) return OperationResult.failure("TA account not found");
                String previous = ta.isDisabled() ? "Disabled" : "Active";
                ta.setDisabled(disabled);
                taDao.update(ta);
                String newState = disabled ? "Disabled" : "Active";
                logAccountAction(adminId, userId, role, disabled ? AccountLog.AccountAction.DISABLE : AccountLog.AccountAction.ENABLE, previous, newState);
            }
            case MO, ADMIN -> {
                Mo mo = moDao.findById(userId).orElse(null);
                if (mo == null) return OperationResult.failure("MO account not found");
                String previous = mo.isDisabled() ? "Disabled" : "Active";
                mo.setDisabled(disabled);
                moDao.update(mo);
                String newState = disabled ? "Disabled" : "Active";
                logAccountAction(adminId, userId, role, disabled ? AccountLog.AccountAction.DISABLE : AccountLog.AccountAction.ENABLE, previous, newState);
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

    public List<AccountLog> findAllAccountLogs() {
        return accountLogDao.findAll();
    }

    private void logAccountAction(String adminId, String targetUserId, Role targetRole, AccountLog.AccountAction action, String previousState, String newState) {
        AccountLog log = new AccountLog();
        log.setLogId(generateLogId());
        log.setAdminId(adminId);
        log.setTargetUserId(targetUserId);
        log.setTargetRole(targetRole);
        log.setAction(action);
        log.setPreviousState(previousState);
        log.setNewState(newState);
        log.setTimestamp(LocalDateTime.now());
        accountLogDao.save(log);
    }

    private String generateLogId() {
        List<String> existing = accountLogDao.findAll().stream()
                .map(AccountLog::getLogId)
                .collect(Collectors.toList());
        return IdGenerator.nextId("log", existing);
    }
}
