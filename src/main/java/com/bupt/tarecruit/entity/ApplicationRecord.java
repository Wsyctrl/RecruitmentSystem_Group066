package com.bupt.tarecruit.entity;

import java.time.LocalDateTime;
import java.util.Objects;

public class ApplicationRecord {
    private String applyId;
    private String taId;
    private String jobId;
    private ApplicationStatus status = ApplicationStatus.PENDING;
    private LocalDateTime updateTime = LocalDateTime.now();
    private LocalDateTime applyTime = LocalDateTime.now();
    private LocalDateTime hiredTime;

    public String getApplyId() {
        return applyId;
    }

    public void setApplyId(String applyId) {
        this.applyId = applyId;
    }

    public String getTaId() {
        return taId;
    }

    public void setTaId(String taId) {
        this.taId = taId;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public LocalDateTime getApplyTime() {
        return applyTime;
    }

    public void setApplyTime(LocalDateTime applyTime) {
        this.applyTime = applyTime;
    }

    public LocalDateTime getHiredTime() {
        return hiredTime;
    }

    public void setHiredTime(LocalDateTime hiredTime) {
        this.hiredTime = hiredTime;
    }

    public boolean isPending() {
        return status == ApplicationStatus.PENDING;
    }

    public boolean isHired() {
        return status == ApplicationStatus.HIRED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ApplicationRecord)) return false;
        ApplicationRecord that = (ApplicationRecord) o;
        return Objects.equals(applyId, that.applyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(applyId);
    }
}
