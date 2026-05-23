package com.bupt.tarecruit.service;

import com.bupt.tarecruit.entity.ApplicationRecord;
import com.bupt.tarecruit.entity.ApplicationStatus;
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

/**
 * Client for Qwen (DashScope) chat-completions used across recruiting AI features.
 * <p>
 * All network calls use the DashScope OpenAI-compatible chat completions endpoint with model
 * {@code qwen-plus}, low temperature (0.2), and role-specific system prompts. JSON-oriented
 * methods expect the model to return parseable JSON; free-text methods return concise English.
 * </p>
 * <p>
 * API key resolution (first non-blank wins): environment variable {@code QWEN_API_KEY},
 * JVM property {@code qwen.api.key}, then classpath resource {@code ai-config.properties}
 * property {@code qwen.api.key}. If none are set, {@link #readApiKey()} throws
 * {@link IllegalStateException}.
 * </p>
 * <p>
 * HTTP failures (status &ge; 400 or empty choices) surface as {@link IOException}.
 * Malformed JSON from the model may throw from {@link org.json.JSONObject} parsing.
 * {@link #fallbackApplicantSummary} provides a local, non-API fallback when applicant
 * summary generation fails or is unavailable.
 * </p>
 */
public class AiService {

    /** DashScope OpenAI-compatible chat completions URL. */
    private static final String API_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
    /** Model id sent in every completion request. */
    private static final String MODEL = "qwen-plus";

    /** Shared client with a 10-second connect timeout; per-request timeout is 20 seconds. */
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * AI-ranked open job for a TA.
     *
     * @param jobId  job identifier returned by the model (matched case-insensitively in callers)
     * @param score  match score 0–100 after {@link #clampScore(int)}
     * @param reason short English justification
     */
    public record JobRecommendation(String jobId, int score, String reason) {
    }

    /**
     * AI-ranked applicant for a job.
     *
     * @param taId   teaching assistant identifier
     * @param score  match score 0–100 after {@link #clampScore(int)}
     * @param reason short English justification
     */
    public record ApplicantRecommendation(String taId, int score, String reason) {
    }

    /**
     * Structured profile fields extracted from CV text.
     *
     * @param fullName        candidate name
     * @param phone           contact phone
     * @param major           academic major
     * @param skills          skills text
     * @param experience      experience text
     * @param selfEvaluation  self-evaluation text
     */
    public record ResumeDraft(String fullName, String phone, String major, String skills, String experience,
                              String selfEvaluation) {
    }

    /**
     * Recommends up to three jobs for a TA using profile and optional preference only (no CV attachment).
     *
     * @param ta         TA profile sent to the model
     * @param jobs       eligible open jobs
     * @param preference free-text TA preference (higher weight in the prompt)
     * @return ranked recommendations, possibly fewer than three if the model returns less
     * @throws IOException          API or transport failure
     * @throws InterruptedException if the HTTP call is interrupted
     * @see #recommendJobsForTa(Ta, List, String, String)
     */
    public List<JobRecommendation> recommendJobsForTa(Ta ta, List<Job> jobs, String preference)
            throws IOException, InterruptedException {
        return recommendJobsForTa(ta, jobs, preference, "");
    }

    /**
     * Recommends up to three jobs for a TA using online profile, optional uploaded resume text,
     * and a preference string that takes priority in scoring.
     *
     * @param ta         TA profile
     * @param jobs       eligible open jobs (callers typically exclude applied/disabled-MO jobs)
     * @param preference free-text preference
     * @param cvText     plain-text resume body extracted from {@code .txt}, {@code .md}, or {@code .pdf};
     *                   blank if none
     * @return parsed recommendations; invalid array elements are skipped
     * @throws IOException          API or transport failure
     * @throws InterruptedException if the HTTP call is interrupted
     */
    public List<JobRecommendation> recommendJobsForTa(Ta ta, List<Job> jobs, String preference, String cvText)
            throws IOException, InterruptedException {
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
                Return at most 3 items sorted by score descending.
                Use both the online profile and the attached resume (when provided). If the resume adds skills or experience not in the online profile, weight those facts in scoring.
                
                TA online profile:
                %s
                
                Attached resume:
                %s
                
                User preference (higher priority):
                %s
                
                Job list:
                %s
                """.formatted(taProfileText(ta), attachedResumeText(cvText), safe(preference), jobsText);
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

    /**
     * Extracts resume fields from CV text without inventing facts; missing fields become empty strings.
     *
     * @param ta     existing TA profile included as context in the prompt
     * @param cvText full CV plain text extracted from {@code .txt}, {@code .md}, or {@code .pdf}
     * @return structured draft for form pre-fill (not persisted by this class)
     * @throws IOException          API or transport failure
     * @throws InterruptedException if the HTTP call is interrupted
     */
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

    /**
     * Produces concise English resume optimization advice for a specific job (bullet sections, not JSON).
     *
     * @param ta  applicant profile
     * @param job target job
     * @return free-text advice from the model
     * @throws IOException          API or transport failure
     * @throws InterruptedException if the HTTP call is interrupted
     */
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

    /**
     * Ranks applicants for a job with default cap of eight results and no MO preference.
     *
     * @param job         job being staffed
     * @param applicants  applicant rows to consider
     * @return up to eight recommendations sorted by the model
     * @throws IOException          API or transport failure
     * @throws InterruptedException if the HTTP call is interrupted
     * @see #recommendApplicantsForJob(Job, List, int, String)
     */
    public List<ApplicantRecommendation> recommendApplicantsForJob(Job job, List<ApplicantDisplay> applicants)
            throws IOException, InterruptedException {
        // Backwards-compatible default: no extra preference, up to 8 results.
        return recommendApplicantsForJob(job, applicants, 8, "");
    }

    /**
     * Ranks applicants for a job with a configurable result cap and optional MO preference text.
     *
     * @param job         job being staffed
     * @param applicants  applicant pool (callers usually pass pending-only)
     * @param maxResults  desired maximum items; clamped to {@code [1, applicants.size()]}
     * @param preference  MO hiring preference; blank is ignored in the prompt
     * @return parsed recommendations
     * @throws IOException          API or transport failure
     * @throws InterruptedException if the HTTP call is interrupted
     */
    public List<ApplicantRecommendation> recommendApplicantsForJob(
            Job job, List<ApplicantDisplay> applicants, int maxResults, String preference)
            throws IOException, InterruptedException {
        int cap = Math.max(1, Math.min(maxResults, applicants.size()));
        String preferenceText = preference == null ? "" : preference.trim();
        String userPrompt = """
                You are a recruiting assistant. Rank applicants for this job.
                Return a JSON array. Each item must include: taId(string), score(int 0-100), reason(string).
                Return at most %d items sorted by score descending.
                The MO has provided extra preference (higher priority when scoring); when blank, ignore it.

                MO preference:
                %s

                Job:
                Name: %s
                Module: %s
                Requirements: %s
                Notes: %s

                Applicant list:
                %s
                """.formatted(cap, preferenceText.isBlank() ? "(none)" : preferenceText,
                safe(job.getJobName()), safe(job.getModuleName()), safe(job.getRequirements()),
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

    /**
     * Finds pending candidates most similar to a benchmark hire for the same job.
     * The benchmark TA must not appear in the returned list.
     *
     * @param job          job context
     * @param benchmark    recently hired or reference TA
     * @param applicants   pending candidates (benchmark excluded by callers)
     * @param maxResults   cap clamped to {@code [1, applicants.size()]}
     * @return similarity-ranked recommendations
     * @throws IOException          API or transport failure
     * @throws InterruptedException if the HTTP call is interrupted
     */
    public List<ApplicantRecommendation> findSimilarApplicants(
            Job job, Ta benchmark, List<ApplicantDisplay> applicants, int maxResults)
            throws IOException, InterruptedException {
        int cap = Math.max(1, Math.min(maxResults, applicants.size()));
        String userPrompt = """
                You are a recruiting assistant. The MO has just hired (or intends to hire) the benchmark applicant below.
                Find the most similar pending candidates in the current applicant pool using the benchmark as the primary reference.
                Do not include the benchmark applicant in the results.
                Return a JSON array. Each item must include: taId(string), score(int 0-100), reason(string).
                Return at most %d items sorted by score descending.
                
                Job:
                Name: %s
                Module: %s
                Requirements: %s
                Notes: %s
                
                Benchmark applicant (hire reference):
                %s
                
                Candidate list:
                %s
                """.formatted(cap, safe(job.getJobName()), safe(job.getModuleName()), safe(job.getRequirements()),
                safe(job.getAdditionalNotes()), taProfileText(benchmark), applicantListText(applicants));
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

    /**
     * Generates 3–5 short English keyword phrases (1–3 words each) for quick job review.
     *
     * @param job job whose title, module, requirements, and notes feed the prompt
     * @return non-empty keyword strings from a JSON string array response
     * @throws IOException          API or transport failure
     * @throws InterruptedException if the HTTP call is interrupted
     */
    public List<String> generateJobKeywords(Job job) throws IOException, InterruptedException {
        String userPrompt = """
                Generate 3 to 5 short English job-requirement keywords for quick hiring review.
                Each keyword must be 1 to 3 words. Do not include sentences or explanations.
                Return a JSON array of strings only.

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

    /**
     * Builds exactly three English insights from last-30-day application aggregates only.
     * Counts are computed locally; the model must not infer data outside that window.
     *
     * @param applications all application records used to derive 30-day metrics
     * @param openJobs     current count of open jobs
     * @return free-text insights (typically numbered paragraphs)
     * @throws IOException          API or transport failure
     * @throws InterruptedException if the HTTP call is interrupted
     */
    public String generate30DayInsights(List<ApplicationRecord> applications, int openJobs) throws IOException, InterruptedException {
        LocalDateTime boundary = LocalDateTime.now().minusDays(30);
        long recentApply = applications.stream()
                .filter(r -> r.getApplyTime() != null && r.getApplyTime().isAfter(boundary))
                .count();
        long recentHired = applications.stream()
                .filter(r -> r.getHiredTime() != null && r.getHiredTime().isAfter(boundary))
                .count();
        long recentPending = applications.stream()
                .filter(r -> r.getApplyTime() != null && r.getApplyTime().isAfter(boundary))
                .filter(r -> r.getStatus() == ApplicationStatus.PENDING)
                .count();
        long recentRejected = applications.stream()
                .filter(r -> r.getApplyTime() != null && r.getApplyTime().isAfter(boundary))
                .filter(r -> r.getStatus() == ApplicationStatus.REJECTED)
                .count();
        long recentWithdrawn = applications.stream()
                .filter(r -> r.getApplyTime() != null && r.getApplyTime().isAfter(boundary))
                .filter(r -> r.getStatus() == ApplicationStatus.WITHDRAWN)
                .count();
        String userPrompt = """
                Based ONLY on the following last-30-day hiring data, generate exactly 3 English insights.
                Each insight must contain a conclusion and one action suggestion, within 2 sentences.
                Do not reference or infer metrics outside this 30-day window.

                Last 30 days:
                - Applications: %d
                - Hires: %d
                - Pending: %d
                - Rejected: %d
                - Withdrawn: %d
                - Current open jobs: %d
                """.formatted(recentApply, recentHired, recentPending, recentRejected, recentWithdrawn, openJobs);
        return chatText(userPrompt);
    }

    /**
     * Generates a one-phrase English strength summary from the TA online profile only.
     *
     * @param ta applicant
     * @return ultra-concise phrase, or {@code Profile incomplete.} when no source material exists
     * @throws IOException          API or transport failure
     * @throws InterruptedException if the HTTP call is interrupted
     * @see #fallbackApplicantSummary(Ta, String)
     */
    public String generateApplicantSummary(Ta ta) throws IOException, InterruptedException {
        return generateApplicantSummary(ta, "");
    }

    /**
     * Generates a one-phrase English strength summary from profile plus optional CV text.
     * Returns {@code Profile incomplete.} without calling the API when all sources are empty.
     *
     * @param ta     applicant
     * @param cvText attached resume plain text extracted from supported attachment formats, or blank
     * @return phrase trimmed from model output, at most ~12 words by prompt rules
     * @throws IOException          API or transport failure
     * @throws InterruptedException if the HTTP call is interrupted
     */
    public String generateApplicantSummary(Ta ta, String cvText) throws IOException, InterruptedException {
        if (!hasApplicantSourceMaterial(ta, cvText)) {
            return incompleteProfileSummary();
        }
        String userPrompt = """
                Write exactly ONE ultra-concise English phrase (at most 12 words) stating this TA applicant's top strengths.

                Rules:
                - Lead with strengths immediately: skills, teaching fit, standout experience. No name, no "is/has/with" openers, no filler.
                - Every word must earn its place; drop articles and padding where meaning stays clear.
                - Use only facts from the online profile, self-evaluation, and attached resume below. Do not invent details.
                - If the online profile is mostly empty but resume or self-evaluation has content, distill strengths from those.
                - If all sources are largely empty, output only: Profile incomplete.
                - Output the phrase only—no quotes, labels, or bullet points.

                Online profile:
                Major: %s
                Skills: %s
                Experience: %s
                Self-evaluation: %s

                Attached resume:
                %s
                """.formatted(
                safe(ta.getMajor()),
                safe(ta.getSkills()),
                safe(ta.getExperience()),
                safe(ta.getSelfEvaluation()),
                attachedResumeText(cvText)
        );
        return chatText(userPrompt).trim();
    }

    /**
     * Local fallback when {@link #generateApplicantSummary(Ta, String)} is unavailable or fails.
     * Prefers skills, then experience, self-evaluation, major, then CV snippet; otherwise
     * {@code Profile incomplete.}
     *
     * @param ta     applicant
     * @param cvText optional resume text extracted from {@code .txt}, {@code .md}, or {@code .pdf}
     * @return truncated phrase suitable for card display
     */
    public static String fallbackApplicantSummary(Ta ta, String cvText) {
        if (!hasApplicantSourceMaterial(ta, cvText)) {
            return incompleteProfileSummary();
        }
        String skills = trimOrEmpty(ta.getSkills());
        if (!skills.isBlank()) {
            return truncateWords(skills.replace(';', ',').replace('|', ','), 12);
        }
        String experience = trimOrEmpty(ta.getExperience());
        if (!experience.isBlank()) {
            return truncateWords(experience, 12);
        }
        String selfEval = trimOrEmpty(ta.getSelfEvaluation());
        if (!selfEval.isBlank()) {
            return truncateWords(selfEval, 12);
        }
        String major = trimOrEmpty(ta.getMajor());
        if (!major.isBlank()) {
            return truncateWords(major, 8);
        }
        String cv = trimOrEmpty(cvText);
        if (!cv.isBlank()) {
            return truncateWords(cv, 12);
        }
        return incompleteProfileSummary();
    }

    /** True when any profile field or CV text has non-blank content. */
    private static boolean hasApplicantSourceMaterial(Ta ta, String cvText) {
        return !trimOrEmpty(ta.getMajor()).isBlank()
                || !trimOrEmpty(ta.getSkills()).isBlank()
                || !trimOrEmpty(ta.getExperience()).isBlank()
                || !trimOrEmpty(ta.getSelfEvaluation()).isBlank()
                || !trimOrEmpty(cvText).isBlank();
    }

    /** Null-safe trim; null becomes empty string. */
    private static String trimOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    /** Fixed message when applicant data is too sparse for summarization. */
    private static String incompleteProfileSummary() {
        return "Profile incomplete.";
    }

    /** Truncates to {@code maxWords} words, appending {@code ...} when shortened. */
    private static String truncateWords(String text, int maxWords) {
        String[] words = text.trim().split("\\s+");
        if (words.length <= maxWords) {
            return text.trim();
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < maxWords; i++) {
            if (i > 0) sb.append(' ');
            sb.append(words[i]);
        }
        sb.append("...");
        return sb.toString();
    }

    /** Serializes applicants into a bullet list for MO ranking prompts. */
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

    /** Formats a TA record as labeled lines for prompts. */
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

    /** Returns CV body or {@code (none uploaded)} when blank. */
    private String attachedResumeText(String cvText) {
        if (cvText == null || cvText.isBlank()) {
            return "(none uploaded)";
        }
        return safe(cvText);
    }

    /**
     * Calls the API with JSON-only system instructions and strips markdown code fences from the reply.
     */
    private String chatJson(String userPrompt) throws IOException, InterruptedException {
        String content = chat(userPrompt, true);
        return content.replace("```json", "").replace("```", "").trim();
    }

    /** Calls the API expecting concise actionable English (non-JSON). */
    private String chatText(String userPrompt) throws IOException, InterruptedException {
        return chat(userPrompt, false);
    }

    /**
     * Posts a chat completion to DashScope and returns the assistant message content.
     *
     * @param userPrompt  user message body
     * @param expectJson  when true, system prompt requires valid JSON only
     */
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

    /** Parses a JSON object from model text (after fence stripping in {@link #chatJson}). */
    private JSONObject asJsonObject(String text) {
        String cleaned = text.trim();
        return new JSONObject(cleaned);
    }

    /**
     * Parses a JSON array; if the root is an object, uses its {@code data} array when present.
     */
    private JSONArray asJsonArray(String text) {
        String cleaned = text.trim();
        if (cleaned.startsWith("{")) {
            JSONObject obj = new JSONObject(cleaned);
            JSONArray arr = obj.optJSONArray("data");
            return arr == null ? new JSONArray() : arr;
        }
        return new JSONArray(cleaned);
    }

    /**
     * Resolves the Qwen API key from environment, system property, or {@code ai-config.properties}.
     *
     * @return non-blank API key
     * @throws IllegalStateException when no key is configured
     */
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

    /** Loads {@code qwen.api.key} from classpath {@code ai-config.properties}, or null if missing. */
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

    /** Null-safe trim for prompt fields. */
    private String safe(String value) {
        return trimOrEmpty(value);
    }

    /** Clamps model scores to the inclusive range 0–100. */
    private int clampScore(int score) {
        return Math.max(0, Math.min(100, score));
    }
}
