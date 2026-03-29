package com.bupt.tarecruit.entity;

import java.util.Objects;

public class Ta {
    private String taId;
    private String password;
    /** Display name */
    private String fullName;
    private String phone;
    private String email;
    private String major;
    private String skills;
    private String experience;
    private String selfEvaluation;
    private boolean disabled;
    private String cvPath;

    public Ta() {
    }

    public Ta(String taId, String password) {
        this.taId = taId;
        this.password = password;
    }

    public String getTaId() {
        return taId;
    }

    public void setTaId(String taId) {
        this.taId = taId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    /** Prefer full name; fallback to user id */
    public String getDisplayLabel() {
        if (fullName != null && !fullName.isBlank()) {
            return fullName;
        }
        return taId;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMajor() {
        return major;
    }

    public void setMajor(String major) {
        this.major = major;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public String getExperience() {
        return experience;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }

    public String getSelfEvaluation() {
        return selfEvaluation;
    }

    public void setSelfEvaluation(String selfEvaluation) {
        this.selfEvaluation = selfEvaluation;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public String getCvPath() {
        return cvPath;
    }

    public void setCvPath(String cvPath) {
        this.cvPath = cvPath;
    }

    public String getStatusLabel() {
        return disabled ? "Disabled" : "Active";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Ta ta)) return false;
        return Objects.equals(taId, ta.taId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taId);
    }
}
