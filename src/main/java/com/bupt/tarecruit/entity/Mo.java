package com.bupt.tarecruit.entity;

import java.util.Objects;

public class Mo {
    private String moId;
    private String password;
    /** Display name */
    private String fullName;
    private String responsibleModules;
    private String phone;
    private String email;
    private boolean disabled;

    public Mo() {
    }

    public Mo(String moId, String password) {
        this.moId = moId;
        this.password = password;
    }

    public String getMoId() {
        return moId;
    }

    public void setMoId(String moId) {
        this.moId = moId;
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
        return moId;
    }

    public String getResponsibleModules() {
        return responsibleModules;
    }

    public void setResponsibleModules(String responsibleModules) {
        this.responsibleModules = responsibleModules;
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

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(moId);
    }

    public String getStatusLabel() {
        return disabled ? "Disabled" : "Active";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Mo mo)) return false;
        return Objects.equals(moId, mo.moId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(moId);
    }
}
