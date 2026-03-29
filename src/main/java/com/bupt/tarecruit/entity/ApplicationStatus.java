package com.bupt.tarecruit.entity;

public enum ApplicationStatus {
    PENDING(0, "Pending"),
    HIRED(1, "Hired"),
    REJECTED(2, "Not hired"),
    WITHDRAWN(3, "Withdrawn");

    private final int code;
    private final String label;

    ApplicationStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static ApplicationStatus fromCode(int code) {
        for (ApplicationStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return PENDING;
    }
}
