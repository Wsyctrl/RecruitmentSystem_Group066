package com.bupt.tarecruit.controller;

import com.bupt.tarecruit.entity.UserSession;

/**
 * Marker for controllers that need authenticated session information.
 */
public interface SessionAware {
    void setSession(UserSession session);
}
