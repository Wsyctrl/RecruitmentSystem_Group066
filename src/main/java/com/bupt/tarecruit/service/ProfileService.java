package com.bupt.tarecruit.service;

import com.bupt.tarecruit.dao.MoDao;
import com.bupt.tarecruit.dao.TaDao;
import com.bupt.tarecruit.entity.Mo;
import com.bupt.tarecruit.entity.Ta;
import com.bupt.tarecruit.util.OperationResult;
import com.bupt.tarecruit.util.ValidationUtil;

import java.util.Optional;

/**
 * Provides business logic for user profile lookup, profile updates,
 * and password changes for TA and MO accounts.
 */
public class ProfileService {

    /**
     * Data access object for TA accounts.
     */
    private final TaDao taDao;

    /**
     * Data access object for MO accounts.
     */
    private final MoDao moDao;

    /**
     * Creates a profile service with the required data access dependencies.
     *
     * @param taDao data access object for TA records
     * @param moDao data access object for MO records
     */
    public ProfileService(TaDao taDao, MoDao moDao) {
        this.taDao = taDao;
        this.moDao = moDao;
    }

    /**
     * Finds a TA user by identifier.
     *
     * @param taId TA user identifier
     * @return optional containing the TA user if found
     */
    public Optional<Ta> findTa(String taId) {
        return taDao.findById(taId);
    }

    /**
     * Finds an MO user by identifier.
     *
     * @param moId MO user identifier
     * @return optional containing the MO user if found
     */
    public Optional<Mo> findMo(String moId) {
        return moDao.findById(moId);
    }

    /**
     * Updates the profile information of a TA user.
     *
     * @param ta TA entity with updated profile data
     * @return operation result containing the updated TA entity
     */
    public OperationResult<Ta> updateTa(Ta ta) {
        taDao.update(ta);
        return OperationResult.success(ta, "Profile saved");
    }

    /**
     * Updates the profile information of an MO user.
     *
     * @param mo MO entity with updated profile data
     * @return operation result containing the updated MO entity
     */
    public OperationResult<Mo> updateMo(Mo mo) {
        moDao.update(mo);
        return OperationResult.success(mo, "Profile saved");
    }

    /**
     * Changes the password of a TA user after validating the current password
     * and confirming the new password.
     *
     * @param taId TA user identifier
     * @param currentPassword current password entered by the user
     * @param newPassword new password entered by the user
     * @param confirmPassword repeated new password entered for confirmation
     * @return operation result describing whether the password update succeeded
     */
    public OperationResult<Void> changeTaPassword(String taId, String currentPassword, String newPassword, String confirmPassword) {
        if (ValidationUtil.isBlank(newPassword)) {
            return OperationResult.failure("New password is required");
        }
        if (!newPassword.equals(confirmPassword)) {
            return OperationResult.failure("New passwords do not match");
        }
        Ta ta = taDao.findById(taId).orElse(null);
        if (ta == null) {
            return OperationResult.failure("Account not found");
        }
        String cur = currentPassword == null ? "" : currentPassword;
        if (!ta.getPassword().equals(cur)) {
            return OperationResult.failure("Current password is incorrect");
        }
        ta.setPassword(newPassword);
        taDao.update(ta);
        return OperationResult.success(null, "Password updated");
    }

    /**
     * Changes the password of an MO user after validating the current password
     * and confirming the new password.
     *
     * @param moId MO user identifier
     * @param currentPassword current password entered by the user
     * @param newPassword new password entered by the user
     * @param confirmPassword repeated new password entered for confirmation
     * @return operation result describing whether the password update succeeded
     */
    public OperationResult<Void> changeMoPassword(String moId, String currentPassword, String newPassword, String confirmPassword) {
        if (ValidationUtil.isBlank(newPassword)) {
            return OperationResult.failure("New password is required");
        }
        if (!newPassword.equals(confirmPassword)) {
            return OperationResult.failure("New passwords do not match");
        }
        Mo mo = moDao.findById(moId).orElse(null);
        if (mo == null) {
            return OperationResult.failure("Account not found");
        }
        String cur = currentPassword == null ? "" : currentPassword;
        if (!mo.getPassword().equals(cur)) {
            return OperationResult.failure("Current password is incorrect");
        }
        mo.setPassword(newPassword);
        moDao.update(mo);
        return OperationResult.success(null, "Password updated");
    }
}