package com.bupt.tarecruit.viewmodel;

import com.bupt.tarecruit.entity.ApplicationRecord;
import com.bupt.tarecruit.entity.Job;
import com.bupt.tarecruit.util.DateTimeUtil;

public class ApplicationDisplay {
    private final ApplicationRecord record;
    private final Job job;

    public ApplicationDisplay(ApplicationRecord record, Job job) {
        this.record = record;
        this.job = job;
    }

    public ApplicationRecord getRecord() {
        return record;
    }

    public Job getJob() {
        return job;
    }

    public String getJobName() {
        return job != null ? job.getJobName() : "";
    }

    public String getApplyId() {
        return record.getApplyId();
    }

    public String getModuleName() {
        return job != null ? job.getModuleName() : "";
    }

    public String getStatusLabel() {
        return record.getStatus().getLabel();
    }

    public String getUpdatedTime() {
        return DateTimeUtil.formatDateTime(record.getUpdateTime());
    }

    public String getJobStartDate() {
        return job != null ? DateTimeUtil.formatDate(job.getStartDate()) : "-";
    }

    public String getJobEndDate() {
        return job != null ? DateTimeUtil.formatDate(job.getEndDate()) : "-";
    }

    public String getHiredTime() {
        return record.getHiredTime() != null ? DateTimeUtil.formatDateTime(record.getHiredTime()) : "-";
    }

    public String getJobPeriod() {
        if (job == null) {
            return "-";
        }
        String start = DateTimeUtil.formatDate(job.getStartDate());
        String end = DateTimeUtil.formatDate(job.getEndDate());
        return start + " - " + end;
    }
}
