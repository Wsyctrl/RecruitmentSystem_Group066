package com.bupt.tarecruit.dao;

import com.bupt.tarecruit.entity.Ta;

import java.util.List;
import java.util.Optional;

/**
 * Data access interface for TA user records.
 * Provides methods for retrieving, creating, and updating TA account data.
 */
public interface TaDao {

    /**
     * Returns all TA user records.
     *
     * @return list of all TA users
     */
    List<Ta> findAll();

    /**
     * Finds a TA user by identifier.
     *
     * @param taId TA user identifier
     * @return optional containing the TA user if found
     */
    Optional<Ta> findById(String taId);

    /**
     * Saves a new TA user record.
     *
     * @param ta TA entity to save
     */
    void save(Ta ta);

    /**
     * Updates an existing TA user record.
     *
     * @param ta TA entity to update
     */
    void update(Ta ta);
}