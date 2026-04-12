package com.bupt.tarecruit.service;

import com.bupt.tarecruit.dao.MoDao;
import com.bupt.tarecruit.dao.TaDao;
import com.bupt.tarecruit.entity.Mo;
import com.bupt.tarecruit.entity.Role;
import com.bupt.tarecruit.entity.Ta;
import com.bupt.tarecruit.entity.UserSession;
import com.bupt.tarecruit.util.IdFormatUtil;
import com.bupt.tarecruit.util.OperationResult;
import com.bupt.tarecruit.util.ValidationUtil;

import java.util.Optional;

/**
 * Provides authentication-related business logic,
 * including user login and account registration.
 */
public class AuthService {

    /**
     * Data access object for TA accounts.
     */
    private final TaDao taDao;

    /**
     * Data access object for MO accounts.
     */
    private final MoDao moDao;

    /**
     * Creates an authentication service with required data access dependencies.
     *
     * @param taDao data access object for TA records
     * @param moDao data access object for MO records
     */
    public AuthService(TaDao taDao, MoDao moDao) {
        this.taDao = taDao;
        this.moDao = moDao;
    }

    /**
     * Authenticates a user with the given credentials and returns a user session on success.
     *
     * @param userId user account identifier
     * @param password raw password entered by the user
     * @return operation result containing the authenticated user session on success
     */
    public OperationResult<UserSession> login(String userId, String password) {
        if (ValidationUtil.isBlank(userId)) {
            return OperationResult.failure("Please enter your username");
        }
        if (ValidationUtil.isBlank(password)) {
            return OperationResult.failure("Please enter your password");
        }

        Optional<Ta> taOptional = taDao.findById(userId);
        if (taOptional.isPresent()) {
            Ta ta = taOptional.get();
            if (!ta.getPassword().equals(password)) {
                return OperationResult.failure("Incorrect password");
            }
            if (ta.isDisabled()) {
                return OperationResult.failure("This account is disabled. Contact an administrator.");
            }
            return OperationResult.success(new UserSession(Role.TA, ta, null), "Signed in");
        }

        Optional<Mo> moOptional = moDao.findById(userId);
        if (moOptional.isPresent()) {
            Mo mo = moOptional.get();
            if (!mo.getPassword().equals(password)) {
                return OperationResult.failure("Incorrect password");
            }
            if (mo.isDisabled()) {
                return OperationResult.failure("This account is disabled. Contact an administrator.");
            }
            Role role = mo.isAdmin() ? Role.ADMIN : Role.MO;
            return OperationResult.success(new UserSession(role, null, mo), "Signed in");
        }
        return OperationResult.failure("Unknown user or incorrect password");
    }

    /**
     * Registers a new TA or MO account after validating the user ID format,
     * password confirmation, and account uniqueness.
     *
     * @param role target account role
     * @param userId user account identifier
     * @param password password entered during registration
     * @param confirmPassword confirmation password entered during registration
     * @return operation result describing whether the registration succeeded
     */
    public OperationResult<Void> register(Role role, String userId, String password, String confirmPassword) {
        if (ValidationUtil.isBlank(userId)) {
            return OperationResult.failure("Username is required");
        }
        if (ValidationUtil.isBlank(password)) {
            return OperationResult.failure("Password is required");
        }
        if (ValidationUtil.isBlank(confirmPassword)) {
            return OperationResult.failure("Please confirm your password");
        }
        if (!password.equals(confirmPassword)) {
            return OperationResult.failure("Passwords do not match");
        }
        boolean exists = taDao.findById(userId).isPresent() || moDao.findById(userId).isPresent();
        if (exists) {
            return OperationResult.failure("Username already exists");
        }
        switch (role) {
            case TA -> {
                if (!IdFormatUtil.isValidTaStudentId(userId)) {
                    return OperationResult.failure(
                            "Invalid student ID format. Use ta plus eight digits where the first four digits after \"ta\" are your enrollment year (e.g. ta20230001).");
                }
                Ta ta = new Ta(userId, password);
                ta.setDisabled(false);
                taDao.save(ta);
            }
            case MO -> {
                if (!IdFormatUtil.isValidMoStaffId(userId)) {
                    return OperationResult.failure(
                            "Invalid staff ID format. Use mo plus eight digits where the first four digits after \"mo\" are your hire year (e.g. mo20160001).");
                }
                Mo mo = new Mo(userId, password);
                mo.setDisabled(false);
                moDao.save(mo);
            }
            default -> throw new IllegalStateException("Unknown role");
        }
        return OperationResult.success(null, "Registration successful. Please sign in.");
    }
}