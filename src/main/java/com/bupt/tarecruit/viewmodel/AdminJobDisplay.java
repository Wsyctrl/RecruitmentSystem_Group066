package com.bupt.tarecruit.viewmodel;

import com.bupt.tarecruit.entity.Job;
import com.bupt.tarecruit.util.DateTimeUtil;

public class AdminJobDisplay {
    private final Job job;
    private final int hiredCount;

    public AdminJobDisplay(Job job, int hiredCount) {
        this.job = job;
        this.hiredCount = hiredCount;
    }

    public String getJobId() {
        return job.getJobId();
    }

    public String getJobName() {
        return job.getJobName();
    }

    public String getMoId() {
        return job.getMoId();
    }

    public String getMoName() {
        if (job.getMoName() == null || job.getMoName().isBlank()) {
            return "-";
        }
        return job.getMoName();
    }

    public String getModuleName() {
        return job.getModuleName();
    }

    public int getNumberOfPositions() {
        return job.getNumberOfPositions();
    }

    public int getHiredCount() {
        return hiredCount;
    }

    public String getStartDateLabel() {
        return DateTimeUtil.formatDate(job.getStartDate());
    }

    public String getEndDateLabel() {
        return DateTimeUtil.formatDate(job.getEndDate());
    }

    public String getRequirements() {
        return job.getRequirements();
    }

    public String getStatusLabel() {
        return job.getStatusLabel();
    }

    public Job getJob() {
        return job;
    }
}
