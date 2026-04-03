package com.bupt.tarecruit.viewmodel;

import com.bupt.tarecruit.entity.Job;
import com.bupt.tarecruit.entity.JobLog;
import com.bupt.tarecruit.util.DateTimeUtil;

public class JobLogDisplay {
    private final JobLog log;
    private final Job job;

    public JobLogDisplay(JobLog log, Job job) {
        this.log = log;
        this.job = job;
    }

    public String getTimestamp() {
        return log.getTimestampLabel();
    }

    public String getAdminId() {
        return log.getAdminId();
    }

    public String getAction() {
        return log.getActionLabel();
    }

    public String getTargetUserId() {
        return log.getJobId();
    }

    public String getJobName() {
        return job != null ? job.getJobName() : "Unknown";
    }

    public String getPreviousState() {
        return log.getPreviousState();
    }

    public String getNewState() {
        return log.getNewState();
    }

    public JobLog getLog() {
        return log;
    }

    public Job getJob() {
        return job;
    }
}
