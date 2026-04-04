package com.bupt.tarecruit.dao;

import com.bupt.tarecruit.entity.AccountLog;

import java.nio.file.Path;
import java.util.List;

public interface AccountLogDao {
    List<AccountLog> findAll();
    void save(AccountLog log);
}
