package com.bupt.tarecruit.entity;

public enum JobStatus {
    OPEN(0, "Open"),
    CLOSED(1, "Closed");

    private final int code;
    private final String label;

    JobStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static JobStatus fromCode(int code) {
        return code == 1 ? CLOSED : OPEN;
    }
}
