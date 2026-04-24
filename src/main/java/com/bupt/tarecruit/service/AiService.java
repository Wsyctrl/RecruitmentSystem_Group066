package com.bupt.tarecruit.service;

import com.bupt.tarecruit.entity.ApplicationRecord;
import com.bupt.tarecruit.entity.Job;
import com.bupt.tarecruit.entity.Ta;
import com.bupt.tarecruit.viewmodel.ApplicantDisplay;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class AiService {

    private static final String API_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
    private static final String MODEL = "qwen-plus";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public record JobRecommendation(String jobId, int score, String reason) {
    }

    public record ApplicantRecommendation(String taId, int score, String reason) {
    }

    public record ResumeDraft(String fullName, String phone, String major, String skills, String experience,
                              String selfEvaluation) {
    }

    public List<JobRecommendation> recommendJobsForTa(Ta ta, List<Job> jobs, String preference) throws IOException, InterruptedException {
        StringBuilder jobsText = new StringBuilder();
        for (Job job : jobs) {
            jobsText.append("- ").append(job.getJobId()).append(" | ")
                    .append(safe(job.getJobName())).append(" | module: ").append(safe(job.getModuleName()))
                    .append(" | requirements: ").append(safe(job.getRequirements()))
                    .append(" | notes: ").append(safe(job.getAdditionalNotes())).append("\n");
        }
        String userPrompt = """
                Recommend the best jobs for this TA.
                Return a JSON array. Each item must include: jobId(string), score(int 0-100), reason(string).
                Return at most 5 items sorted by score descending.
                
                TA profile:
                %s
                
                User preference (higher priority):
                %s
                
                Job list:
                %s
                """.formatted(taProfileText(ta), safe(preference), jobsText);
        String content = chatJson(userPrompt);
        JSONArray arr = asJsonArray(content);
        List<JobRecommendation> result = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            result.add(new JobRecommendation(
                    o.optString("jobId", ""),
                    clampScore(o.optInt("score", 0)),
                    o.optString("reason", "")
            ));
        }
        return result;
    }

    public ResumeDraft draftResumeFromCv(Ta ta, String cvText) throws IOException, InterruptedException {
        String userPrompt = """
                You are a resume extraction assistant. Extract data from CV text, preserve original wording when possible, do not fabricate facts.
                Return a JSON object with fields:
                fullName, phone, major, skills, experience, selfEvaluation
                If a field is missing in CV, return an empty string for that field.
                
                Existing profile values:
                %s
                
                CV text:
                %s
                """.formatted(taProfileText(ta), safe(cvText));
        JSONObject obj = asJsonObject(chatJson(userPrompt));
        return new ResumeDraft(
                obj.optString("fullName", ""),
                obj.optString("phone", ""),
                obj.optString("major", ""),
                obj.optString("skills", ""),
                obj.optString("experience", ""),
                obj.optString("selfEvaluation", "")
        );
    }

    public String suggestResumeOptimization(Ta ta, Job job) throws IOException, InterruptedException {
        String userPrompt = """
                Provide resume optimization advice for the target job.
                Output concise English bullet points in three sections:
                1) Main gaps
                2) Immediate rewrite suggestions
                3) Example phrasing
                
                TA profile:
                %s
                
                Job:
                Name: %s
                Module: %s
                Requirements: %s
                Notes: %s
                """.formatted(taProfileText(ta), safe(job.getJobName()), safe(job.getModuleName()),
                safe(job.getRequirements()), safe(job.getAdditionalNotes()));
        return chatText(userPrompt);
    }

    public List<ApplicantRecommendation> recommendApplicantsForJob(Job job, List<ApplicantDisplay> applicants)
            throws IOException, InterruptedException {
        String userPrompt = """
                You are a recruiting assistant. Rank applicants for this job.
                Return a JSON array. Each item must include: taId(string), score(int 0-100), reason(string).
                Return at most 8 items sorted by score descending.
                
                Job:
                Name: %s
                Module: %s
                Requirements: %s
                Notes: %s
                
                Applicant list:
                %s
                """.formatted(safe(job.getJobName()), safe(job.getModuleName()), safe(job.getRequirements()),
                safe(job.getAdditionalNotes()), applicantListText(applicants));
        JSONArray arr = asJsonArray(chatJson(userPrompt));
        List<ApplicantRecommendation> result = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            result.add(new ApplicantRecommendation(
                    o.optString("taId", ""),
                    clampScore(o.optInt("score", 0)),
                    o.optString("reason", "")
            ));
        }
        return result;
    }

    public List<ApplicantRecommendation> findSimilarApplicants(Job job, Ta benchmark, List<ApplicantDisplay> applicants)
            throws IOException, InterruptedException {
        String userPrompt = """
                You are a recruiting assistant. Use the benchmark applicant and find the most similar candidates in the current applicant pool.
                Return a JSON array. Each item must include: taId(string), score(int 0-100), reason(string).
                Return at most 5 items sorted by score descending.
                
                Job:
                Name: %s
                Module: %s
                Requirements: %s
                
                Benchmark applicant:
                %s
                
                Candidate list:
                %s
                """.formatted(safe(job.getJobName()), safe(job.getModuleName()), safe(job.getRequirements()),
                taProfileText(benchmark), applicantListText(applicants));
        JSONArray arr = asJsonArray(chatJson(userPrompt));
        List<ApplicantRecommendation> result = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            result.add(new ApplicantRecommendation(
                    o.optString("taId", ""),
                    clampScore(o.optInt("score", 0)),
                    o.optString("reason", "")
            ));
        }
        return result;
    }

    public List<String> generateJobKeywords(Job job) throws IOException, InterruptedException {
        String userPrompt = """
                Generate 5 to 10 English job requirement keywords for quick hiring review.
                Return a JSON array of strings.
                
                Job:
                Name: %s
                Module: %s
                Requirements: %s
                Notes: %s
                """.formatted(safe(job.getJobName()), safe(job.getModuleName()), safe(job.getRequirements()), safe(job.getAdditionalNotes()));
        JSONArray arr = asJsonArray(chatJson(userPrompt));
        List<String> result = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            String keyword = arr.optString(i, "").trim();
            if (!keyword.isEmpty()) {
                result.add(keyword);
            }
        }
        return result;
    }

    public String generate30DayInsights(List<Job> jobs, List<ApplicationRecord> applications, int hiredCount, int openJobs) throws IOException, InterruptedException {
        LocalDateTime boundary = LocalDateTime.now().minusDays(30);
        long recentApply = applications.stream()
                .filter(r -> r.getApplyTime() != null && r.getApplyTime().isAfter(boundary))
                .count();
        long recentHired = applications.stream()
                .filter(r -> r.getHiredTime() != null && r.getHiredTime().isAfter(boundary))
                .count();
        String userPrompt = """
                Based on the following 30-day hiring data, generate exactly 3 English insights.
                Each insight must contain a conclusion and one action suggestion, within 2 sentences.
                
                Metrics:
                - Applications in last 30 days: %d
                - Hires in last 30 days: %d
                - Current open jobs: %d
                - Total jobs in history: %d
                - Total hires in history: %d
                """.formatted(recentApply, recentHired, openJobs, jobs.size(), hiredCount);
        return chatText(userPrompt);
    }

    private String applicantListText(List<ApplicantDisplay> applicants) {
        StringBuilder text = new StringBuilder();
        for (ApplicantDisplay display : applicants) {
            Ta ta = display.getTa();
            if (ta == null) continue;
            text.append("- taId=").append(ta.getTaId())
                    .append(", name=").append(safe(ta.getDisplayLabel()))
                    .append(", major=").append(safe(ta.getMajor()))
                    .append(", skills=").append(safe(ta.getSkills()))
                    .append(", experience=").append(safe(ta.getExperience()))
                    .append(", selfEval=").append(safe(ta.getSelfEvaluation()))
                    .append("\n");
        }
        return text.toString();
    }

    private String taProfileText(Ta ta) {
        return """
                taId: %s
                fullName: %s
                major: %s
                skills: %s
                experience: %s
                selfEvaluation: %s
                """.formatted(
                safe(ta.getTaId()),
                safe(ta.getDisplayLabel()),
                safe(ta.getMajor()),
                safe(ta.getSkills()),
                safe(ta.getExperience()),
                safe(ta.getSelfEvaluation())
        );
    }

    private String chatJson(String userPrompt) throws IOException, InterruptedException {
        String content = chat(userPrompt, true);
        return content.replace("```json", "").replace("```", "").trim();
    }

    private String chatText(String userPrompt) throws IOException, InterruptedException {
        return chat(userPrompt, false);
    }

    private String chat(String userPrompt, boolean expectJson) throws IOException, InterruptedException {
        String apiKey = readApiKey();
        JSONObject body = new JSONObject();
        body.put("model", MODEL);
        body.put("temperature", 0.2);
        JSONArray messages = new JSONArray();
        messages.put(new JSONObject()
                .put("role", "system")
                .put("content", expectJson
                        ? "You are a strict recruiting AI assistant. You must return valid JSON only, with no extra text."
                        : "You are a strict recruiting AI assistant. Return concise, actionable English output."));
        messages.put(new JSONObject().put("role", "user").put("content", userPrompt));
        body.put("messages", messages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IOException("AI API request failed: HTTP " + response.statusCode() + " - " + response.body());
        }
        JSONObject root = new JSONObject(response.body());
        JSONArray choices = root.optJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new IOException("AI API returned no choices");
        }
        return choices.getJSONObject(0).getJSONObject("message").optString("content", "").trim();
    }

    private JSONObject asJsonObject(String text) {
        String cleaned = text.trim();
        return new JSONObject(cleaned);
    }

    private JSONArray asJsonArray(String text) {
        String cleaned = text.trim();
        if (cleaned.startsWith("{")) {
            JSONObject obj = new JSONObject(cleaned);
            JSONArray arr = obj.optJSONArray("data");
            return arr == null ? new JSONArray() : arr;
        }
        return new JSONArray(cleaned);
    }

    private String readApiKey() {
        String apiKey = System.getenv("QWEN_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = System.getProperty("qwen.api.key");
        }
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = readApiKeyFromResource();
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Qwen API key is missing. Set QWEN_API_KEY, -Dqwen.api.key, or configure ai-config.properties.");
        }
        return apiKey;
    }

    private String readApiKeyFromResource() {
        try (InputStream in = AiService.class.getClassLoader().getResourceAsStream("ai-config.properties")) {
            if (in == null) {
                return null;
            }
            Properties props = new Properties();
            props.load(in);
            return props.getProperty("qwen.api.key");
        } catch (IOException ex) {
            return null;
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private int clampScore(int score) {
        return Math.max(0, Math.min(100, score));
    }
}
