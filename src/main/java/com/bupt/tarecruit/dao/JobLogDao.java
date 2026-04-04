package com.bupt.tarecruit.dao;

import com.bupt.tarecruit.entity.JobLog;

import java.util.List;

public interface JobLogDao {
    List<JobLog> findAll();
    void save(JobLog log);
}
