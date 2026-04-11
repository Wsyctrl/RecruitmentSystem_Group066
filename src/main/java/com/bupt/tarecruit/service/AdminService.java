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

/**
 * Provides administrator-level operations for account management,
 * job management, and account action logging.
 */
public class AdminService {

    /**
     * Default password assigned when an administrator resets a user password.
     */
    private static final String DEFAULT_PASSWORD = "Pass@123";

    /**
     * Data access object for TA accounts.
     */
    private final TaDao taDao;

    /**
     * Data access object for MO accounts.
     */
    private final MoDao moDao;

    /**
     * Data access object for job records.
     */
    private final JobDao jobDao;

    /**
     * Data access object for account action logs.
     */
    private final AccountLogDao accountLogDao;

    /**
     * Creates an admin service with required data access dependencies.
     *
     * @param taDao data access object for TA records
     * @param moDao data access object for MO records
     * @param jobDao data access object for job records
     * @param accountLogDao data access object for account log records
     */
    public AdminService(TaDao taDao, MoDao moDao, JobDao jobDao, AccountLogDao accountLogDao) {
        this.taDao = taDao;
        this.moDao = moDao;
        this.jobDao = jobDao;
        this.accountLogDao = accountLogDao;
    }

    /**
     * Returns all TA accounts.
     *
     * @return list of all TA users
     */
    public List<Ta> findAllTa() {
        return taDao.findAll();
    }

    /**
     * Returns all MO accounts.
     *
     * @return list of all MO users
     */
    public List<Mo> findAllMo() {
        return moDao.findAll();
    }

    /**
     * Resets the password of a TA or MO account to the default password
     * and records the action in the account log.
     *
     * @param role target user role
     * @param userId target user identifier
     * @param adminId administrator identifier performing the action
     * @return operation result describing whether the reset succeeded
     */
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

    /**
     * Enables or disables a TA or MO account and records the action in the account log.
     *
     * @param role target user role
     * @param userId target user identifier
     * @param disabled target disabled status
     * @param adminId administrator identifier performing the action
     * @return operation result describing whether the status update succeeded
     */
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

    /**
     * Returns all job records.
     *
     * @return list of all jobs
     */
    public List<Job> findAllJobs() {
        return jobDao.findAll();
    }

    /**
     * Closes an open job from the administrator job management view.
     *
     * @param jobId target job identifier
     * @return operation result describing whether the close action succeeded
     */
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

    /**
     * Returns all recorded account action logs.
     *
     * @return list of account logs
     */
    public List<AccountLog> findAllAccountLogs() {
        return accountLogDao.findAll();
    }

    /**
     * Creates and stores an account action log entry for an administrator operation.
     *
     * @param adminId administrator identifier
     * @param targetUserId target user identifier
     * @param targetRole target user role
     * @param action action performed on the target account
     * @param previousState previous account state description
     * @param newState new account state description
     */
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

    /**
     * Generates the next unique log identifier for a new account log entry.
     *
     * @return generated log identifier
     */
    private String generateLogId() {
        List<String> existing = accountLogDao.findAll().stream()
                .map(AccountLog::getLogId)
                .collect(Collectors.toList());
        return IdGenerator.nextId("log", existing);
    }
}