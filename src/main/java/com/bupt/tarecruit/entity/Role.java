package com.bupt.tarecruit.entity;

/**
 * Supported login roles in the TA recruitment system.
 * Determines which profile type is active in a {@link UserSession} and
 * which features are available after authentication.
 */
public enum Role {

    /**
     * Teaching assistant applicant or hiree.
     */
    TA,

    /**
     * Module organizer who creates and manages job postings.
     */
    MO,

    /**
     * System administrator with elevated account and job management privileges.
     */
    ADMIN
}
