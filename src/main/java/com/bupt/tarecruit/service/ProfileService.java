package com.bupt.tarecruit.service;

import com.bupt.tarecruit.dao.MoDao;
import com.bupt.tarecruit.dao.TaDao;
import com.bupt.tarecruit.entity.Mo;
import com.bupt.tarecruit.entity.Ta;
import com.bupt.tarecruit.util.OperationResult;
import com.bupt.tarecruit.util.ValidationUtil;

import java.util.Optional;

public class ProfileService {

    private final TaDao taDao;
    private final MoDao moDao;

    public ProfileService(TaDao taDao, MoDao moDao) {
        this.taDao = taDao;
        this.moDao = moDao;
    }

    public Optional<Ta> findTa(String taId) {
        return taDao.findById(taId);
    }

    public Optional<Mo> findMo(String moId) {
        return moDao.findById(moId);
    }

    public OperationResult<Ta> updateTa(Ta ta) {
        taDao.update(ta);
        return OperationResult.success(ta, "Profile saved");
    }

    public OperationResult<Mo> updateMo(Mo mo) {
        moDao.update(mo);
        return OperationResult.success(mo, "Profile saved");
    }

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
