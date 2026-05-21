package com.bupt.tarecruit.viewmodel;

import com.bupt.tarecruit.entity.Job;
import com.bupt.tarecruit.entity.Mo;

import java.util.List;
import java.util.stream.Collectors;

/**
 * View model for presenting {@link Mo} (module organizer) records in the administrator MO table.
 * Aggregates associated job postings for class-count and detail expansion.
 */
public class AdminMoDisplay {

    /** Underlying module organizer entity. */
    private final Mo mo;

    /** Jobs posted by this module organizer. */
    private final List<Job> jobs;

    /**
     * Creates a display wrapper for an administrator MO row.
     *
     * @param mo   module organizer entity; must not be {@code null}
     * @param jobs list of jobs owned by this MO; must not be {@code null}
     */
    public AdminMoDisplay(Mo mo, List<Job> jobs) {
        this.mo = mo;
        this.jobs = jobs;
    }

    /**
     * Table column: unique module organizer identifier.
     *
     * @return MO user ID
     */
    public String getMoId() {
        return mo.getMoId();
    }

    /**
     * Table column: full name of the module organizer.
     *
     * @return display full name
     */
    public String getFullName() {
        return mo.getFullName();
    }

    /**
     * Table column: contact phone number.
     *
     * @return phone number
     */
    public String getPhone() {
        return mo.getPhone();
    }

    /**
     * Table column: contact email address.
     *
     * @return email address
     */
    public String getEmail() {
        return mo.getEmail();
    }

    /**
     * Table column: account status (display label).
     *
     * @return localized or formatted status label
     */
    public String getStatusLabel() {
        return mo.getStatusLabel();
    }

    /**
     * Table column: number of class/job postings associated with this MO.
     *
     * @return count of jobs in {@link #jobs}
     */
    public int getClassesCount() {
        return jobs.size();
    }

    /**
     * Returns the list of jobs for this module organizer (detail panels, expansion).
     *
     * @return jobs posted by this MO; not modified by callers
     */
    public List<Job> getJobs() {
        return jobs;
    }

    /**
     * Returns the wrapped module organizer entity for detail views or actions.
     *
     * @return underlying {@link Mo}
     */
    public Mo getMo() {
        return mo;
    }
}
