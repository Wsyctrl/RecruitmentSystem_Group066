package com.bupt.tarecruit.entity;

import java.util.Optional;

/**
 * Immutable snapshot of the currently authenticated user for use in controllers.
 * Holds the active {@link Role} and at most one populated profile
 * ({@link Ta} or {@link Mo}) depending on how the user signed in.
 *
 * @param role      authenticated user's role
 * @param taProfile teaching assistant profile when {@code role} is {@link Role#TA}; may be {@code null}
 * @param moProfile module organizer profile when {@code role} is {@link Role#MO} or {@link Role#ADMIN}; may be {@code null}
 */
public record UserSession(Role role, Ta taProfile, Mo moProfile) {

    /**
     * Returns the teaching assistant profile as an {@link Optional}.
     * Empty when {@link #taProfile} is {@code null}.
     *
     * @return optional TA profile
     */
    public Optional<Ta> taOptional() {
        return Optional.ofNullable(taProfile);
    }

    /**
     * Returns the module organizer profile as an {@link Optional}.
     * Empty when {@link #moProfile} is {@code null}.
     *
     * @return optional MO profile
     */
    public Optional<Mo> moOptional() {
        return Optional.ofNullable(moProfile);
    }

    /**
     * Resolves a display name for the authenticated user.
     * Uses {@link Ta#getDisplayLabel()} when {@link #role} is {@link Role#TA} and a TA profile is present;
     * otherwise uses {@link Mo#getDisplayLabel()} when an MO profile is present;
     * falls back to {@code "User"} when no profile is available.
     *
     * @return display name for the current session
     */
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
