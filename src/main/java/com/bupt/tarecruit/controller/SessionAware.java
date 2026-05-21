package com.bupt.tarecruit.controller;

import com.bupt.tarecruit.entity.UserSession;

/**
 * Controllers that receive the authenticated {@link UserSession} after navigation
 * from login or registration.
 */
public interface SessionAware {
    /**
     * Injects the active session and triggers dashboard data loading in implementors.
     *
     * @param session authenticated TA, MO, or admin session; must not be null for signed-in flows
     */
    void setSession(UserSession session);
}
