package com.bupt.tarecruit.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EntityEnumTest {

    @Test
    void jobStatusFromCode() {
        assertEquals(JobStatus.OPEN, JobStatus.fromCode(0));
        assertEquals(JobStatus.CLOSED, JobStatus.fromCode(1));
        assertEquals(JobStatus.OPEN, JobStatus.fromCode(99));
    }

    @Test
    void applicationStatusFromCode() {
        assertEquals(ApplicationStatus.PENDING, ApplicationStatus.fromCode(0));
        assertEquals(ApplicationStatus.WITHDRAWN, ApplicationStatus.fromCode(1));
        assertEquals(ApplicationStatus.HIRED, ApplicationStatus.fromCode(2));
        assertEquals(ApplicationStatus.REJECTED, ApplicationStatus.fromCode(3));
        assertEquals(ApplicationStatus.PENDING, ApplicationStatus.fromCode(99));
    }

    @Test
    void applicationRecordStatusHelpers() {
        ApplicationRecord pending = new ApplicationRecord();
        pending.setStatus(ApplicationStatus.PENDING);
        assertTrue(pending.isPending());
        assertFalse(pending.isHired());

        ApplicationRecord hired = new ApplicationRecord();
        hired.setStatus(ApplicationStatus.HIRED);
        assertTrue(hired.isHired());
        assertFalse(hired.isPending());
    }
}
