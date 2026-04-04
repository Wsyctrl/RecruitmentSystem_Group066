package com.bupt.tarecruit.viewmodel;

import com.bupt.tarecruit.entity.Job;
import com.bupt.tarecruit.entity.Mo;

import java.util.List;
import java.util.stream.Collectors;

public class AdminMoDisplay {
    private final Mo mo;
    private final List<Job> jobs;

    public AdminMoDisplay(Mo mo, List<Job> jobs) {
        this.mo = mo;
        this.jobs = jobs;
    }

    public String getMoId() {
        return mo.getMoId();
    }

    public String getFullName() {
        return mo.getFullName();
    }

    public String getPhone() {
        return mo.getPhone();
    }

    public String getEmail() {
        return mo.getEmail();
    }

    public String getStatusLabel() {
        return mo.getStatusLabel();
    }

    public int getClassesCount() {
        return jobs.size();
    }

    public List<Job> getJobs() {
        return jobs;
    }

    public Mo getMo() {
        return mo;
    }
}
