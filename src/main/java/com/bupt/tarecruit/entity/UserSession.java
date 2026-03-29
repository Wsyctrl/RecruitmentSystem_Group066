package com.bupt.tarecruit.entity;

import java.util.Optional;

/**
 * Keeps authenticated user information for controllers.
 */
public record UserSession(Role role, Ta taProfile, Mo moProfile) {

    public Optional<Ta> taOptional() {
        return Optional.ofNullable(taProfile);
    }

    public Optional<Mo> moOptional() {
        return Optional.ofNullable(moProfile);
    }

    public String getDisplayName() {
        if (role == Role.TA && taProfile != null) {
            return taProfile.getDisplayLabel();
        }
        if (moProfile != null) {
            return moProfile.getDisplayLabel();
        }
        return "User";
    }
}
