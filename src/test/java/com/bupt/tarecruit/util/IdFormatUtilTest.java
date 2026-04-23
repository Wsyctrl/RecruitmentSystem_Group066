package com.bupt.tarecruit.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IdFormatUtilTest {

    @Test
    void buptEmail() {
        assertTrue(IdFormatUtil.isValidBuptEmail("ta20230001@bupt.edu.cn"));
        assertTrue(IdFormatUtil.isValidBuptEmail("admin@bupt.edu.cn"));
        assertFalse(IdFormatUtil.isValidBuptEmail("ta20230001@qq.com"));
        assertFalse(IdFormatUtil.isValidBuptEmail("invalid-email"));
    }

    @Test
    void compatibilityWrappers() {
        assertTrue(IdFormatUtil.isValidTaStudentId("ta20230001@bupt.edu.cn"));
        assertTrue(IdFormatUtil.isValidMoStaffId("evans@bupt.edu.cn"));
        assertFalse(IdFormatUtil.isValidTaStudentId("ta20230001"));
    }

    @Test
    void adminFlag() {
        assertTrue(IdFormatUtil.isAdminUserId("admin@bupt.edu.cn"));
        assertTrue(IdFormatUtil.isAdminUserId("Admin@bupt.edu.cn"));
    }
}
