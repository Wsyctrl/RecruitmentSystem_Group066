package com.bupt.tarecruit.dao;

import com.bupt.tarecruit.entity.AccountLog;

import java.util.List;

/**
 * Data access interface for account log records.
 * Provides methods for retrieving and storing administrator account operation logs.
 */
public interface AccountLogDao {

    /**
     * Returns all account log records.
     *
     * @return list of all account logs
     */
    List<AccountLog> findAll();

    /**
     * Saves a new account log record.
     *
     * @param log account log entity to save
     */
    void save(AccountLog log);
}