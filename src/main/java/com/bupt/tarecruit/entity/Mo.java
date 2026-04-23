package com.bupt.tarecruit.entity;

import java.util.Objects;

public class Mo {
    private String email;
    private String password;
    /** Display name */
    private String fullName;
    private String responsibleModules;
    private String phone;
    private boolean disabled;

    public Mo() {
    }

    public Mo(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getMoId() {
        return email;
    }

    public void setMoId(String moId) {
        this.email = moId;
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

    /** Prefer full name; fallback to email identity. */
    public String getDisplayLabel() {
        if (fullName != null && !fullName.isBlank()) {
            return fullName;
        }
        return email;
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
        return "admin@bupt.edu.cn".equalsIgnoreCase(email);
    }

    public String getStatusLabel() {
        return disabled ? "Disabled" : "Active";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Mo mo)) return false;
        return Objects.equals(email, mo.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email);
    }
}
