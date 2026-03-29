package com.bupt.tarecruit.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IdFormatUtilTest {

    @Test
    void taStudentId() {
        assertTrue(IdFormatUtil.isValidTaStudentId("ta20230001"));
        assertTrue(IdFormatUtil.isValidTaStudentId("ta19991234"));
        assertFalse(IdFormatUtil.isValidTaStudentId("ta001"));
        assertFalse(IdFormatUtil.isValidTaStudentId("TA20230001"));
        assertFalse(IdFormatUtil.isValidTaStudentId("student20230001"));
    }

    @Test
    void moStaffId() {
        assertTrue(IdFormatUtil.isValidMoStaffId("mo20160001"));
        assertFalse(IdFormatUtil.isValidMoStaffId("admin"));
        assertFalse(IdFormatUtil.isValidMoStaffId("mo001"));
    }

    @Test
    void adminFlag() {
        assertTrue(IdFormatUtil.isAdminUserId("admin"));
        assertTrue(IdFormatUtil.isAdminUserId("Admin"));
    }
}
