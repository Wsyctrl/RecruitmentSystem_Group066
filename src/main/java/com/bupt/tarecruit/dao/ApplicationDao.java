package com.bupt.tarecruit.dao;

import com.bupt.tarecruit.entity.ApplicationRecord;

import java.util.List;
import java.util.Optional;

/**
 * Data access interface for job application records.
 * Provides methods for retrieving, creating, and updating application data.
 */
public interface ApplicationDao {

    /**
     * Returns all application records.
     *
     * @return list of all applications
     */
    List<ApplicationRecord> findAll();

    /**
     * Returns all application records submitted by a specific TA user.
     *
     * @param taId TA user identifier
     * @return list of applications submitted by the TA user
     */
    List<ApplicationRecord> findByTaId(String taId);

    /**
     * Returns all application records for a specific job.
     *
     * @param jobId job identifier
     * @return list of applications for the job
     */
    List<ApplicationRecord> findByJobId(String jobId);

    /**
     * Finds an application record by identifier.
     *
     * @param applyId application identifier
     * @return optional containing the application record if found
     */
    Optional<ApplicationRecord> findById(String applyId);

    /**
     * Saves a new application record.
     *
     * @param record application entity to save
     */
    void save(ApplicationRecord record);

    /**
     * Updates an existing application record.
     *
     * @param record application entity to update
     */
    void update(ApplicationRecord record);
}