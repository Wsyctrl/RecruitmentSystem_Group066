package com.bupt.tarecruit.entity;

import java.util.Objects;

/**
 * Teaching assistant (TA) account in the recruitment system.
 * The primary key is the {@link #email} address, which also serves as {@code taId}.
 * Holds profile, skills, CV path, and optional AI-generated summary.
 * Equality is based solely on {@link #email}.
 */
public class Ta {

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
     * Contact phone number.
     */
    private String phone;

    /**
     * Academic major or field of study.
     */
    private String major;

    /**
     * Free-text description of relevant skills.
     */
    private String skills;

    /**
     * Free-text description of prior teaching or work experience.
     */
    private String experience;

    /**
     * Applicant's self-evaluation or personal statement.
     */
    private String selfEvaluation;

    /**
     * When {@code true}, the account cannot log in or perform actions.
     */
    private boolean disabled;

    /**
     * File system path to the uploaded curriculum vitae (CV).
     */
    private String cvPath;

    /**
     * AI-generated one-line profile summary cached on the TA record.
     */
    private String aiSummary;

    /**
     * Creates an empty teaching assistant with default field values.
     */
    public Ta() {
    }

    /**
     * Creates a teaching assistant with the given email and password.
     *
     * @param email    account email and identifier
     * @param password account password
     */
    public Ta(String email, String password) {
        this.email = email;
        this.password = password;
    }

    /**
     * Returns the teaching assistant identifier, which is the {@link #email} address.
     *
     * @return teaching assistant identifier (email)
     */
    public String getTaId() {
        return email;
    }

    /**
     * Sets the teaching assistant identifier by updating {@link #email}.
     *
     * @param taId teaching assistant identifier (email)
     */
    public void setTaId(String taId) {
        this.email = taId;
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
     * @return display label for this teaching assistant
     */
    public String getDisplayLabel() {
        if (fullName != null && !fullName.isBlank()) {
            return fullName;
        }
        return email;
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
     * Returns the academic major or field of study.
     *
     * @return major
     */
    public String getMajor() {
        return major;
    }

    /**
     * Sets the academic major or field of study.
     *
     * @param major major
     */
    public void setMajor(String major) {
        this.major = major;
    }

    /**
     * Returns the free-text skills description.
     *
     * @return skills text
     */
    public String getSkills() {
        return skills;
    }

    /**
     * Sets the free-text skills description.
     *
     * @param skills skills text
     */
    public void setSkills(String skills) {
        this.skills = skills;
    }

    /**
     * Returns the free-text experience description.
     *
     * @return experience text
     */
    public String getExperience() {
        return experience;
    }

    /**
     * Sets the free-text experience description.
     *
     * @param experience experience text
     */
    public void setExperience(String experience) {
        this.experience = experience;
    }

    /**
     * Returns the applicant's self-evaluation or personal statement.
     *
     * @return self-evaluation text
     */
    public String getSelfEvaluation() {
        return selfEvaluation;
    }

    /**
     * Sets the applicant's self-evaluation or personal statement.
     *
     * @param selfEvaluation self-evaluation text
     */
    public void setSelfEvaluation(String selfEvaluation) {
        this.selfEvaluation = selfEvaluation;
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
     * Returns the file system path to the uploaded CV.
     *
     * @return CV file path
     */
    public String getCvPath() {
        return cvPath;
    }

    /**
     * Sets the file system path to the uploaded CV.
     *
     * @param cvPath CV file path
     */
    public void setCvPath(String cvPath) {
        this.cvPath = cvPath;
    }

    /**
     * Returns the cached AI-generated profile summary.
     *
     * @return AI summary text, or {@code null} if not generated
     */
    public String getAiSummary() {
        return aiSummary;
    }

    /**
     * Sets the cached AI-generated profile summary.
     *
     * @param aiSummary AI summary text
     */
    public void setAiSummary(String aiSummary) {
        this.aiSummary = aiSummary;
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
     * Compares this teaching assistant to another object for equality.
     * Two accounts are equal when they share the same non-null {@link #email}.
     *
     * @param o object to compare
     * @return {@code true} if the other object is a {@link Ta} with the same email
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Ta ta)) return false;
        return Objects.equals(email, ta.email);
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
