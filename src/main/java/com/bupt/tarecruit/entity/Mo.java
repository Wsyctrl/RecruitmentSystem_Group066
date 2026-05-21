package com.bupt.tarecruit.entity;

import java.util.Objects;

/**
 * Module organizer (MO) account in the recruitment system.
 * The primary key is the {@link #email} address, which also serves as {@code moId}.
 * Supports profile fields, disable flag, and built-in admin detection.
 * Equality is based solely on {@link #email}.
 */
public class Mo {

    /**
     * Email address used as the unique account identifier and login name.
     */
    private String email;

    /**
     * Hashed or plain-text password stored for authentication (per persistence layer).
     */
    private String password;

    /**
     * Full display name shown in the user interface when available.
     */
    private String fullName;

    /**
     * Comma-separated or free-form list of academic modules this organizer manages.
     */
    private String responsibleModules;

    /**
     * Contact phone number.
     */
    private String phone;

    /**
     * When {@code true}, the account cannot log in or perform actions.
     */
    private boolean disabled;

    /**
     * Creates an empty module organizer with default field values.
     */
    public Mo() {
    }

    /**
     * Creates a module organizer with the given email and password.
     *
     * @param email    account email and identifier
     * @param password account password
     */
    public Mo(String email, String password) {
        this.email = email;
        this.password = password;
    }

    /**
     * Returns the module organizer identifier, which is the {@link #email} address.
     *
     * @return module organizer identifier (email)
     */
    public String getMoId() {
        return email;
    }

    /**
     * Sets the module organizer identifier by updating {@link #email}.
     *
     * @param moId module organizer identifier (email)
     */
    public void setMoId(String moId) {
        this.email = moId;
    }

    /**
     * Returns the account password.
     *
     * @return password value
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the account password.
     *
     * @param password password value
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Returns the full display name.
     *
     * @return full name, or {@code null} if not set
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * Sets the full display name.
     *
     * @param fullName full name
     */
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    /**
     * Returns the best available label for UI display.
     * Prefers non-blank {@link #fullName}; falls back to {@link #email} when the name is absent.
     *
     * @return display label for this organizer
     */
    public String getDisplayLabel() {
        if (fullName != null && !fullName.isBlank()) {
            return fullName;
        }
        return email;
    }

    /**
     * Returns the modules this organizer is responsible for.
     *
     * @return responsible modules text
     */
    public String getResponsibleModules() {
        return responsibleModules;
    }

    /**
     * Sets the modules this organizer is responsible for.
     *
     * @param responsibleModules responsible modules text
     */
    public void setResponsibleModules(String responsibleModules) {
        this.responsibleModules = responsibleModules;
    }

    /**
     * Returns the contact phone number.
     *
     * @return phone number
     */
    public String getPhone() {
        return phone;
    }

    /**
     * Sets the contact phone number.
     *
     * @param phone phone number
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * Returns the account email address.
     *
     * @return email address
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the account email address.
     *
     * @param email email address
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Indicates whether this account has been disabled by an administrator.
     *
     * @return {@code true} if the account is disabled
     */
    public boolean isDisabled() {
        return disabled;
    }

    /**
     * Sets whether this account is disabled.
     *
     * @param disabled {@code true} to disable the account
     */
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    /**
     * Indicates whether this account is the built-in system administrator.
     * Matches {@code admin@bupt.edu.cn} against {@link #email}, case-insensitively.
     *
     * @return {@code true} if this organizer is the admin account
     */
    public boolean isAdmin() {
        return "admin@bupt.edu.cn".equalsIgnoreCase(email);
    }

    /**
     * Returns a human-readable account status label for UI display.
     *
     * @return {@code "Disabled"} when {@link #disabled} is {@code true}, otherwise {@code "Active"}
     */
    public String getStatusLabel() {
        return disabled ? "Disabled" : "Active";
    }

    /**
     * Compares this organizer to another object for equality.
     * Two organizers are equal when they share the same non-null {@link #email}.
     *
     * @param o object to compare
     * @return {@code true} if the other object is a {@link Mo} with the same email
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Mo mo)) return false;
        return Objects.equals(email, mo.email);
    }

    /**
     * Returns a hash code based on {@link #email}.
     *
     * @return hash code consistent with {@link #equals(Object)}
     */
    @Override
    public int hashCode() {
        return Objects.hash(email);
    }
}
