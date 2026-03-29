package com.bupt.tarecruit.dao;

import com.bupt.tarecruit.entity.Job;

import java.util.List;
import java.util.Optional;

public interface JobDao {
    List<Job> findAll();

    Optional<Job> findById(String jobId);

    void save(Job job);

    void update(Job job);
}
