package com.bupt.tarecruit.viewmodel;

import com.bupt.tarecruit.entity.Job;
import com.bupt.tarecruit.util.DateTimeUtil;

public class TaJobDisplay {
    private final Job job;
    private final int applicantsCount;
    private final int hiredCount;

    public TaJobDisplay(Job job, int applicantsCount, int hiredCount) {
        this.job = job;
        this.applicantsCount = applicantsCount;
        this.hiredCount = hiredCount;
    }

    public String getJobId() {
        return job.getJobId();
    }

    public String getJobName() {
        return job.getJobName();
    }

    public String getModuleName() {
        return job.getModuleName();
    }

    public String getMoName() {
        return job.getMoName();
    }

    public int getNumberOfPositions() {
        return job.getNumberOfPositions();
    }

    public String getPostedDate() {
        // Start date is used as posted date
        return DateTimeUtil.formatDate(job.getStartDate());
    }

    public int getApplicantsCount() {
        return applicantsCount;
    }

    public int getHiredCount() {
        return hiredCount;
    }

    public Job getJob() {
        return job;
    }
}
