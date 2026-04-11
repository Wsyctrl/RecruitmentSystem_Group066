package com.bupt.tarecruit.dao;

import com.bupt.tarecruit.entity.Mo;

import java.util.List;
import java.util.Optional;

/**
 * Data access interface for MO user records.
 * Provides methods for retrieving, creating, and updating MO account data.
 */
public interface MoDao {

    /**
     * Returns all MO user records.
     *
     * @return list of all MO users
     */
    List<Mo> findAll();

    /**
     * Finds an MO user by identifier.
     *
     * @param moId MO user identifier
     * @return optional containing the MO user if found
     */
    Optional<Mo> findById(String moId);

    /**
     * Saves a new MO user record.
     *
     * @param mo MO entity to save
     */
    void save(Mo mo);

    /**
     * Updates an existing MO user record.
     *
     * @param mo MO entity to update
     */
    void update(Mo mo);
}