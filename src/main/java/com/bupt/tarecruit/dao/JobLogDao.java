package com.bupt.tarecruit.dao;

import com.bupt.tarecruit.entity.JobLog;

import java.util.List;

/**
 * Data access interface for job log records.
 * Provides methods for retrieving and storing job management operation logs.
 */
public interface JobLogDao {

    /**
     * Returns all job log records.
     *
     * @return list of all job logs
     */
    List<JobLog> findAll();

    /**
     * Saves a new job log record.
     *
     * @param log job log entity to save
     */
    void save(JobLog log);
}