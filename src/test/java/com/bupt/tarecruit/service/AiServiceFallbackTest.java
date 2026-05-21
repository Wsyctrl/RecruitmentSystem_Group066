package com.bupt.tarecruit.service;

import com.bupt.tarecruit.entity.Ta;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AiServiceFallbackTest {

    @Test
    void fallbackSummaryUsesSkillsWhenPresent() {
        Ta ta = new Ta("ta@bupt.edu.cn", "x");
        ta.setSkills("Java; Python; teaching");
        String summary = AiService.fallbackApplicantSummary(ta, "");
        assertFalse(summary.isBlank());
        assertFalse(summary.toLowerCase().contains("incomplete"));
    }

    @Test
    void fallbackSummaryIncompleteWhenNoMaterial() {
        Ta ta = new Ta("ta@bupt.edu.cn", "x");
        String summary = AiService.fallbackApplicantSummary(ta, "");
        assertEquals("Profile incomplete.", summary);
    }

    @Test
    void fallbackSummaryUsesCvWhenProfileEmpty() {
        Ta ta = new Ta("ta@bupt.edu.cn", "x");
        String cv = "Experienced tutor with strong communication skills and lab supervision";
        String summary = AiService.fallbackApplicantSummary(ta, cv);
        assertFalse(summary.isBlank());
        assertTrue(summary.split("\\s+").length <= 12);
    }
}
