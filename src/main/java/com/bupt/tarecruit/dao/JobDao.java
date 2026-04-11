package com.bupt.tarecruit.dao;

import com.bupt.tarecruit.entity.Job;

import java.util.List;
import java.util.Optional;

/**
 * Data access interface for job records.
 * Provides methods for retrieving, creating, and updating job data.
 */
public interface JobDao {

    /**
     * Returns all job records.
     *
     * @return list of all jobs
     */
    List<Job> findAll();

    /**
     * Finds a job by identifier.
     *
     * @param jobId job identifier
     * @return optional containing the job if found
     */
    Optional<Job> findById(String jobId);

    /**
     * Saves a new job record.
     *
     * @param job job entity to save
     */
    void save(Job job);

    /**
     * Updates an existing job record.
     *
     * @param job job entity to update
     */
    void update(Job job);
}