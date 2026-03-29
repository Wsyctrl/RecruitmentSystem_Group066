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

public class AuthService {

    private final TaDao taDao;
    private final MoDao moDao;

    public AuthService(TaDao taDao, MoDao moDao) {
        this.taDao = taDao;
        this.moDao = moDao;
    }

    public OperationResult<UserSession> login(String userId, String password) {
        ValidationUtil.requireNonBlank(userId, "Please enter your username");
        ValidationUtil.requireNonBlank(password, "Please enter your password");

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

    public OperationResult<Void> register(Role role, String userId, String password, String confirmPassword) {
        ValidationUtil.requireNonBlank(userId, "Username is required");
        ValidationUtil.requireNonBlank(password, "Password is required");
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
