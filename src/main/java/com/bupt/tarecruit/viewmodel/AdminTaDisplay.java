package com.bupt.tarecruit.viewmodel;

import com.bupt.tarecruit.entity.ApplicationRecord;
import com.bupt.tarecruit.entity.ApplicationStatus;
import com.bupt.tarecruit.entity.Job;
import com.bupt.tarecruit.entity.Ta;
import com.bupt.tarecruit.util.DateTimeUtil;
import com.bupt.tarecruit.util.WorkloadRules;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class AdminTaDisplay {
    private final Ta ta;
    private final List<ApplicationRecord> applications;
    private final List<Job> allJobs;

    public AdminTaDisplay(Ta ta, List<ApplicationRecord> applications, List<Job> allJobs) {
        this.ta = ta;
        this.applications = applications;
        this.allJobs = allJobs;
    }

    public String getTaId() {
        return ta.getTaId();
    }

    public String getFullName() {
        return ta.getFullName();
    }

    public String getPhone() {
        return ta.getPhone();
    }

    public String getEmail() {
        return ta.getEmail();
    }

    public String getStatusLabel() {
        return ta.getStatusLabel();
    }

    public int getAppliedCount() {
        return (int) applications.stream()
                .filter(a -> a.getStatus() != ApplicationStatus.WITHDRAWN)
                .count();
    }

    public int getHiredCount() {
        return (int) applications.stream()
                .filter(a -> a.getStatus() == ApplicationStatus.HIRED)
                .count();
    }

    public int getCurrentOngoingJobsCount() {
        LocalDate now = LocalDate.now();
        return (int) applications.stream()
                .filter(a -> a.getStatus() == ApplicationStatus.HIRED)
                .map(a -> findJob(a.getJobId()))
                .filter(job -> job != null
                        && job.getStartDate() != null
                        && job.getEndDate() != null
                        && now.isAfter(job.getStartDate())
                        && now.isBefore(job.getEndDate()))
                .count();
    }

    public boolean isOverConcurrentThreshold() {
        return getCurrentOngoingJobsCount() > WorkloadRules.CONCURRENT_JOB_WARNING_THRESHOLD;
    }

    public List<JobApplicationInfo> getAppliedJobs() {
        return applications.stream()
                .filter(a -> a.getStatus() != ApplicationStatus.WITHDRAWN)
                .map(a -> {
                    Job job = findJob(a.getJobId());
                    return new JobApplicationInfo(job, a.getStatus(), a.getApplyTime());
                })
                .collect(Collectors.toList());
    }

    public List<JobApplicationInfo> getHiredJobs() {
        return applications.stream()
                .filter(a -> a.getStatus() == ApplicationStatus.HIRED)
                .map(a -> {
                    Job job = findJob(a.getJobId());
                    return new JobApplicationInfo(job, a.getStatus(), a.getHiredTime());
                })
                .collect(Collectors.toList());
    }

    private Job findJob(String jobId) {
        return allJobs.stream()
                .filter(j -> j.getJobId().equalsIgnoreCase(jobId))
                .findFirst()
                .orElse(null);
    }

    public Ta getTa() {
        return ta;
    }

    public static class JobApplicationInfo {
        private final Job job;
        private final ApplicationStatus status;
        private final java.time.LocalDateTime time;

        public JobApplicationInfo(Job job, ApplicationStatus status, java.time.LocalDateTime time) {
            this.job = job;
            this.status = status;
            this.time = time;
        }

        public String getJobName() {
            return job != null ? job.getJobName() : "Unknown";
        }

        public String getJobId() {
            return job != null ? job.getJobId() : "Unknown";
        }

        public String getTime() {
            return time != null ? DateTimeUtil.formatDateTime(time) : "-";
        }

        public String getWorkPeriod() {
            if (job == null) {
                return "-";
            }
            String start = DateTimeUtil.formatDate(job.getStartDate());
            String end = DateTimeUtil.formatDate(job.getEndDate());
            return start + " to " + end;
        }

        public Job getJob() {
            return job;
        }
    }
}
