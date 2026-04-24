package com.bupt.tarecruit.controller;

import com.bupt.tarecruit.entity.Job;
import com.bupt.tarecruit.entity.Mo;
import com.bupt.tarecruit.entity.Ta;
import com.bupt.tarecruit.entity.UserSession;
import com.bupt.tarecruit.service.AiService;
import com.bupt.tarecruit.service.ApplicationService;
import com.bupt.tarecruit.service.JobService;
import com.bupt.tarecruit.util.DateTimeUtil;
import com.bupt.tarecruit.util.DialogUtil;
import com.bupt.tarecruit.util.FileStorageHelper;
import com.bupt.tarecruit.util.OperationResult;
import com.bupt.tarecruit.viewmodel.ApplicationDisplay;
import com.bupt.tarecruit.viewmodel.TaJobDisplay;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.concurrent.Task;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
/**
 * Controller for the TA dashboard view.
 * Manages job browsing, job applications, profile updates,
 * CV upload and download, and account-related actions.
 */
public class TaDashboardController extends BaseController implements SessionAware {

    private record ProfileDraft(String fullName, String phone, String major, String skills, String experience, String selfEvaluation) {
    }

    private UserSession session;
    private boolean guestMode;
    private boolean suppressTabGuard;
    private boolean handlingProfileNavigation;
    private ProfileDraft persistedProfileDraft;
    private long resumeAdviceContextVersion = 0L;
    private Task<String> activeResumeAdviceTask;
    private final ObservableList<TaJobDisplay> jobItems = FXCollections.observableArrayList();
    private FilteredList<TaJobDisplay> filteredJobs;
    private final ObservableList<ApplicationDisplay> applicationItems = FXCollections.observableArrayList();

    @FXML
    private Label welcomeLabel;
    @FXML
    private Button authButton;
    @FXML
    private TabPane tabPane;
    @FXML
    private Tab browseJobsTab;
    @FXML
    private Tab myApplicationsTab;
    @FXML
    private Tab myProfileTab;
    @FXML
    private TextField jobSearchField;
    @FXML
    private TableView<TaJobDisplay> jobTable;
    @FXML
    private Label jobNameLabel;
    @FXML
    private Label jobMoNameLabel;
    @FXML
    private Label jobModuleLabel;
    @FXML
    private Label jobPositionsLabel;
    @FXML
    private Label jobDateLabel;
    @FXML
    private TextArea jobRequirementsArea;
    @FXML
    private TextArea jobNotesArea;
    @FXML
    private Button applyButton;
    @FXML
    private TextArea aiJobPreferenceField;
    @FXML
    private TextArea aiJobRecommendationArea;
    @FXML
    private TextArea aiResumeAdviceArea;

    @FXML
    private TableView<ApplicationDisplay> applicationTable;
    @FXML
    private Button withdrawButton;

    @FXML
    private TextField fullNameField;
    @FXML
    private TextField phoneField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField majorField;
    @FXML
    private TextArea skillsArea;
    @FXML
    private TextArea experienceArea;
    @FXML
    private TextArea selfEvalArea;
    @FXML
    private Label cvPathLabel;
    @FXML
    private Button downloadCvButton;
    @FXML
    private Label aiFillStatusLabel;
    @FXML
    private PasswordField currentPasswordField;
    @FXML
    private PasswordField newPasswordField;
    @FXML
    private PasswordField confirmPasswordField;
/**
 * Performs controller-specific initialization after shared dependencies
 * have been injected. Sets up job filtering, table bindings, and selection listeners.
 */
    @Override
    protected void onInit() {
        filteredJobs = new FilteredList<>(jobItems, job -> true);
        jobTable.setItems(filteredJobs);
        jobTable.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (old != selected) {
                invalidateResumeAdviceContext();
            }
            updateJobDetails(selected);
        });
        jobSearchField.textProperty().addListener((obs, old, value) -> applyJobFilter(value));
        applicationTable.setItems(applicationItems);
        if (tabPane != null) {
            tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
                if (newTab == null || handlingProfileNavigation) {
                    return;
                }
                if (!guestMode) {
                    boolean allowSwitch = handleProfileTabSwitch(oldTab, newTab);
                    if (!allowSwitch) {
                        return;
                    }
                }
                if (oldTab == browseJobsTab && newTab != browseJobsTab) {
                    invalidateResumeAdviceContext();
                }
                if (suppressTabGuard || !guestMode) {
                    return;
                }
                if (newTab == myApplicationsTab || newTab == myProfileTab) {
                    requireLoginAndRedirect("Please sign in first.");
                }
            });
        }
        updateJobDetails(null);
    }
/**
 * Sets the current user session and loads the initial dashboard data.
 *
 * @param session current authenticated user session
 */
    @Override
    public void setSession(UserSession session) {
        this.session = session;
        this.guestMode = false;
        welcomeLabel.setText("Welcome, " + session.getDisplayName());
        if (authButton != null) {
            authButton.setText("Log out");
        }
        loadInitialData();
        selectTab(myApplicationsTab);
    }

    public void enterGuestMode() {
        this.session = null;
        this.guestMode = true;
        welcomeLabel.setText("Browse jobs as guest");
        if (authButton != null) {
            authButton.setText("Log in");
        }
        refreshJobs();
        applicationItems.clear();
        selectTab(browseJobsTab);
    }
/**
 * Loads all initial dashboard data, including jobs,
 * existing applications, and profile information.
 */
    private void loadInitialData() {
        if (!guestMode) {
            refreshApplications();
        } else {
            applicationItems.clear();
        }
        refreshJobs();
        updateJobDetails(jobTable.getSelectionModel().getSelectedItem());
        if (!guestMode) {
            loadProfile();
        }
    }
/**
 * Reloads all open jobs and converts them into display models
 * with applicant and hired-count information for the UI.
 */
    private void refreshJobs() {
        JobService jobService = services.jobService();
        ApplicationService applicationService = services.applicationService();

        List<Job> jobs = jobService.findOpenJobs();
        // Fill MO display names for UI rendering.
        for (Job job : jobs) {
            String moId = job.getMoId();
            services.profileService().findMo(moId)
                    .map(Mo::getDisplayLabel)
                    .ifPresent(job::setMoName);
        }

        // Create TaJobDisplay items with applicant and hired counts
        List<TaJobDisplay> displayItems = jobs.stream()
                .map(job -> new TaJobDisplay(
                        job,
                        applicationService.findActiveApplicationsForJob(job.getJobId()).size(),
                        applicationService.countHiredForJob(job.getJobId())
                ))
                .collect(Collectors.toList());

        jobItems.setAll(displayItems);
        if (!jobItems.isEmpty()) {
            jobTable.getSelectionModel().selectFirst();
        } else {
            updateJobDetails(null);
        }
    }
/**
 * Reloads all active applications submitted by the current TA user.
 */
    private void refreshApplications() {
        if (guestMode || session == null) {
            applicationItems.clear();
            return;
        }
        String taId = session.taOptional().map(Ta::getTaId).orElse("");
        ApplicationService applicationService = services.applicationService();
        Map<String, Job> jobMap = services.jobService().findAllJobs().stream()
                .collect(Collectors.toMap(Job::getJobId, Function.identity(), (a, b) -> a));
        applicationItems.setAll(applicationService.findActiveApplicationsForTa(taId).stream()
                .map(record -> new ApplicationDisplay(record, jobMap.get(record.getJobId())))
                .collect(Collectors.toList()));
        applicationTable.refresh();
        updateJobDetails(jobTable.getSelectionModel().getSelectedItem());
    }
/**
 * Loads the current TA user's profile data into the profile form fields.
 */
    private void loadProfile() {
        Ta ta = session.taOptional().orElse(null);
        if (ta == null) {
            return;
        }
        fullNameField.setText(ta.getFullName() == null ? "" : ta.getFullName());
        phoneField.setText(ta.getPhone());
        emailField.setText(ta.getEmail());
        majorField.setText(ta.getMajor());
        skillsArea.setText(ta.getSkills());
        experienceArea.setText(ta.getExperience());
        selfEvalArea.setText(ta.getSelfEvaluation());
        persistedProfileDraft = snapshotProfileForm();
        updateCvUi(ta);
        clearPasswordFields();
    }
/**
 * Updates the CV-related UI controls according to whether
 * the current TA user has uploaded a CV file.
 *
 * @param ta current TA user
 */
    private void updateCvUi(Ta ta) {
        boolean hasCv = ta.getCvPath() != null && !ta.getCvPath().isBlank();
        cvPathLabel.setText(hasCv ? "Uploaded" : "None");
        if (downloadCvButton != null) {
            downloadCvButton.setVisible(hasCv);
            downloadCvButton.setManaged(hasCv);
        }
    }
/**
 * Clears all password input fields in the profile section.
 */
    private void clearPasswordFields() {
        if (currentPasswordField != null) {
            currentPasswordField.clear();
        }
        if (newPasswordField != null) {
            newPasswordField.clear();
        }
        if (confirmPasswordField != null) {
            confirmPasswordField.clear();
        }
    }
/**
 * Applies a keyword filter to the visible job list.
 *
 * @param keyword search keyword entered by the user
 */
    private void applyJobFilter(String keyword) {
        if (filteredJobs == null) {
            return;
        }
        String lower = keyword == null ? "" : keyword.toLowerCase();
        filteredJobs.setPredicate(display -> {
            Job job = display.getJob();
            return containsIgnoreCase(job.getJobName(), lower)
                    || containsIgnoreCase(job.getModuleName(), lower)
                    || containsIgnoreCase(job.getRequirements(), lower);
        });
    }
/**
 * Checks whether the source text contains the given keyword,
 * ignoring case differences.
 *
 * @param source source text to search in
 * @param keyword keyword to search for
 * @return true if the source contains the keyword; false otherwise
 */
    private boolean containsIgnoreCase(String source, String keyword) {
        if (source == null) {
            return false;
        }
        return source.toLowerCase().contains(keyword);
    }
/**
 * Returns a placeholder when the given date text is null or blank.
 *
 * @param value formatted date text
 * @return the original value or "-" when the value is empty
 */
    private String formatDateOrPlaceholder(String value) {
        return (value == null || value.isBlank()) ? "-" : value;
    }
/**
 * Returns an empty string when the input value is null.
 *
 * @param value source text value
 * @return non-null text value
 */
    private String safeText(String value) {
        return value == null ? "" : value;
    }
/**
 * Updates the job detail panel based on the currently selected job.
 *
 * @param display selected job display model, or null to clear the detail view
 */
    private void updateJobDetails(TaJobDisplay display) {
        if (display == null) {
            jobNameLabel.setText("Select a job");
            jobMoNameLabel.setText("-");
            jobModuleLabel.setText("-");
            jobPositionsLabel.setText("-");
            jobDateLabel.setText("-");
            jobRequirementsArea.clear();
            jobNotesArea.clear();
            applyButton.setDisable(true);
            applyButton.setText("Apply");
            return;
        }
        Job job = display.getJob();
        jobNameLabel.setText(job.getJobName());
        String moEmail = safeText(job.getMoId());
        String moName = safeText(job.getMoName());
        if (moName.isBlank() || moName.equalsIgnoreCase(moEmail)) {
            jobMoNameLabel.setText(moEmail);
        } else {
            jobMoNameLabel.setText(moName + " (" + moEmail + ")");
        }
        jobModuleLabel.setText(job.getModuleName());
        jobPositionsLabel.setText("Positions: " + job.getNumberOfPositions());
        String start = formatDateOrPlaceholder(DateTimeUtil.formatDate(job.getStartDate()));
        String end = formatDateOrPlaceholder(DateTimeUtil.formatDate(job.getEndDate()));
        jobDateLabel.setText(start + " to " + end);
        jobRequirementsArea.setText(safeText(job.getRequirements()));
        jobNotesArea.setText(safeText(job.getAdditionalNotes()));
        boolean alreadyApplied = hasApplied(job.getJobId());
        applyButton.setDisable(!job.isOpen() || alreadyApplied);
        applyButton.setText(alreadyApplied ? "Already Applied" : "Apply");
    }
/**
 * Checks whether the current TA user has already applied for the given job.
 *
 * @param jobId target job identifier
 * @return true if an application for the job already exists; false otherwise
 */
    private boolean hasApplied(String jobId) {
        return applicationItems.stream().anyMatch(display -> display.getRecord().getJobId().equalsIgnoreCase(jobId));
    }

    @FXML
    private void handleApply() {
        if (guestMode) {
            requireLoginAndRedirect("Please sign in first.");
            return;
        }
        TaJobDisplay display = jobTable.getSelectionModel().getSelectedItem();
        if (display == null) {
            DialogUtil.error("Please select a job first", navigator.getPrimaryStage());
            return;
        }
        Job job = display.getJob();
        String taId = session.taOptional().map(Ta::getTaId).orElse("");
        OperationResult<?> result = services.applicationService().applyForJob(taId, job);
        if (result.success()) {
            DialogUtil.info(result.message(), navigator.getPrimaryStage());
            refreshApplications();
            refreshJobs(); // Refresh to update applicant counts
            updateJobDetails(display);
        } else {
            DialogUtil.error(result.message(), navigator.getPrimaryStage());
        }
    }

    @FXML
    private void handleWithdraw() {
        if (guestMode) {
            requireLoginAndRedirect("Please sign in first.");
            return;
        }
        ApplicationDisplay display = applicationTable.getSelectionModel().getSelectedItem();
        if (display == null) {
            DialogUtil.error("Please select an application to withdraw", navigator.getPrimaryStage());
            return;
        }
        if (!display.getRecord().isPending()) {
            DialogUtil.error("Only pending applications can be withdrawn", navigator.getPrimaryStage());
            return;
        }
        if (DialogUtil.confirm("Withdraw this application?", navigator.getPrimaryStage())) {
            OperationResult<Void> result = services.applicationService()
                    .withdraw(display.getRecord().getApplyId(), display.getRecord().getTaId());
            if (result.success()) {
                refreshApplications();
                refreshJobs();
            } else {
                DialogUtil.error(result.message(), navigator.getPrimaryStage());
            }
        }
    }

    @FXML
    private void handleSaveProfile() {
        if (guestMode) {
            requireLoginAndRedirect("Please sign in first.");
            return;
        }
        Ta ta = session.taOptional().orElse(null);
        if (ta == null) {
            return;
        }
        ta.setFullName(fullNameField.getText() == null ? "" : fullNameField.getText().trim());
        ta.setPhone(phoneField.getText());
        ta.setMajor(majorField.getText());
        ta.setSkills(skillsArea.getText());
        ta.setExperience(experienceArea.getText());
        ta.setSelfEvaluation(selfEvalArea.getText());
        OperationResult<Ta> result = services.profileService().updateTa(ta);
        if (result.success()) {
            persistedProfileDraft = snapshotProfileForm();
            welcomeLabel.setText("Welcome, " + session.getDisplayName());
            DialogUtil.info(result.message(), navigator.getPrimaryStage());
        } else {
            DialogUtil.error(result.message(), navigator.getPrimaryStage());
        }
    }

    @FXML
    private void handleChangePassword() {
        if (guestMode) {
            requireLoginAndRedirect("Please sign in first.");
            return;
        }
        Ta ta = session.taOptional().orElse(null);
        if (ta == null) {
            return;
        }
        String cur = currentPasswordField.getText() == null ? "" : currentPasswordField.getText();
        String nw = newPasswordField.getText() == null ? "" : newPasswordField.getText();
        String cf = confirmPasswordField.getText() == null ? "" : confirmPasswordField.getText();
        OperationResult<Void> result = services.profileService()
                .changeTaPassword(ta.getTaId(), cur, nw, cf);
        if (result.success()) {
            ta.setPassword(nw);
            clearPasswordFields();
            DialogUtil.info(result.message(), navigator.getPrimaryStage());
        } else {
            DialogUtil.error(result.message(), navigator.getPrimaryStage());
        }
    }

    @FXML
    private void handleUploadCv() {
        if (guestMode) {
            requireLoginAndRedirect("Please sign in first.");
            return;
        }
        Ta ta = session.taOptional().orElse(null);
        if (ta == null) {
            return;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose TXT resume");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text files (*.txt)", "*.txt"));
        File selected = fileChooser.showOpenDialog(navigator.getPrimaryStage());
        if (selected == null) {
            return;
        }
        if (!selected.getName().toLowerCase().endsWith(".txt")) {
            DialogUtil.error("Only .txt files are allowed", navigator.getPrimaryStage());
            return;
        }
        FileStorageHelper helper = services.fileStorageHelper();
        String storedPath = helper.saveCv(ta.getTaId(), selected);
        ta.setCvPath(storedPath);
        services.profileService().updateTa(ta);
        updateCvUi(ta);
        DialogUtil.info("CV uploaded", navigator.getPrimaryStage());
    }

    @FXML
    private void handleDownloadCv() {
        if (guestMode) {
            requireLoginAndRedirect("Please sign in first.");
            return;
        }
        Ta ta = session.taOptional().orElse(null);
        if (ta == null) {
            return;
        }
        if (ta.getCvPath() == null || ta.getCvPath().isBlank()) {
            DialogUtil.error("No CV has been uploaded yet", navigator.getPrimaryStage());
            return;
        }

        Path source = services.fileStorageHelper().resolveCvFile(ta.getTaId(), ta.getCvPath());
        if (!Files.isRegularFile(source)) {
            DialogUtil.error("CV file not found on disk", navigator.getPrimaryStage());
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialFileName(ta.getTaId() + "_cv.txt");
        File dest = fileChooser.showSaveDialog(navigator.getPrimaryStage());
        if (dest == null) {
            return;
        }

        try {
            Files.copy(source, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            DialogUtil.info("CV saved to: " + dest.getAbsolutePath(), navigator.getPrimaryStage());
        } catch (Exception e) {
            DialogUtil.error("Download failed: " + e.getMessage(), navigator.getPrimaryStage());
        }
    }

    @FXML
    private void handleAuthAction() {
        if (guestMode) {
            navigator.showLogin();
            return;
        }
        if (navigator.isTaPortal()) {
            navigator.showTaGuestDashboard();
        } else {
            navigator.showLogin();
        }
    }

    @FXML
    private void handleRefreshJobs() {
        refreshJobs();
    }

    private void requireLoginAndRedirect(String message) {
        navigator.showLoginWithNotice(message);
    }

    private void selectTab(Tab tab) {
        if (tabPane == null || tab == null) {
            return;
        }
        suppressTabGuard = true;
        tabPane.getSelectionModel().select(tab);
        suppressTabGuard = false;
    }

    private ProfileDraft snapshotProfileForm() {
        return new ProfileDraft(
                safeText(fullNameField.getText()).trim(),
                safeText(phoneField.getText()).trim(),
                safeText(majorField.getText()).trim(),
                safeText(skillsArea.getText()).trim(),
                safeText(experienceArea.getText()).trim(),
                safeText(selfEvalArea.getText()).trim()
        );
    }

    private boolean hasUnsavedProfileChanges() {
        return persistedProfileDraft != null && !persistedProfileDraft.equals(snapshotProfileForm());
    }

    private boolean handleProfileTabSwitch(Tab oldTab, Tab newTab) {
        if (myProfileTab == null || tabPane == null) {
            return true;
        }
        if (oldTab == myProfileTab && newTab != myProfileTab) {
            handlingProfileNavigation = true;
            tabPane.getSelectionModel().select(myProfileTab);
            handlingProfileNavigation = false;
            if (hasUnsavedProfileChanges()) {
                boolean saveNow = DialogUtil.confirmYesNo(
                        "You have unsaved profile changes. Save before leaving this page?",
                        navigator.getPrimaryStage()
                );
                if (saveNow) {
                    handleSaveProfile();
                } else {
                    loadProfile();
                }
            }
            handlingProfileNavigation = true;
            tabPane.getSelectionModel().select(newTab);
            handlingProfileNavigation = false;
            return false;
        }
        if (newTab == myProfileTab) {
            handlingProfileNavigation = true;
            loadProfile();
            handlingProfileNavigation = false;
        }
        return true;
    }

    private void invalidateResumeAdviceContext() {
        resumeAdviceContextVersion++;
        if (activeResumeAdviceTask != null && activeResumeAdviceTask.isRunning()) {
            activeResumeAdviceTask.cancel(true);
        }
        if (aiResumeAdviceArea != null) {
            aiResumeAdviceArea.clear();
        }
    }

    private boolean isResumeAdviceContextValid(long contextToken, String jobIdSnapshot) {
        if (contextToken != resumeAdviceContextVersion) {
            return false;
        }
        if (tabPane == null || tabPane.getSelectionModel().getSelectedItem() != browseJobsTab) {
            return false;
        }
        TaJobDisplay selected = jobTable.getSelectionModel().getSelectedItem();
        return selected != null && selected.getJob().getJobId().equalsIgnoreCase(jobIdSnapshot);
    }

    @FXML
    private void handleAiRecommendJobs() {
        if (guestMode) {
            requireLoginAndRedirect("Please sign in first.");
            return;
        }
        Ta ta = session.taOptional().orElse(null);
        if (ta == null) {
            return;
        }
        List<Job> jobs = services.jobService().findOpenJobs();
        if (jobs.isEmpty()) {
            aiJobRecommendationArea.setText("No open jobs are available.");
            return;
        }
        aiJobRecommendationArea.setText("AI is analyzing...");
        String preference = aiJobPreferenceField == null ? "" : aiJobPreferenceField.getText();
        Task<List<AiService.JobRecommendation>> task = new Task<>() {
            @Override
            protected List<AiService.JobRecommendation> call() throws Exception {
                return services.aiService().recommendJobsForTa(ta, jobs, preference);
            }
        };
        task.setOnSucceeded(evt -> {
            List<AiService.JobRecommendation> items = task.getValue();
            if (items == null || items.isEmpty()) {
                aiJobRecommendationArea.setText("No AI recommendations were returned.");
                return;
            }
            Map<String, Job> jobMap = jobs.stream().collect(Collectors.toMap(Job::getJobId, Function.identity(), (a, b) -> a));
            String text = items.stream()
                    .map(item -> {
                        Job job = jobMap.get(item.jobId());
                        String title = job == null ? item.jobId() : job.getJobName() + " (" + item.jobId() + ")";
                        return "• " + title + " | Match Score: " + item.score() + "\n  " + item.reason();
                    })
                    .collect(Collectors.joining("\n\n"));
            aiJobRecommendationArea.setText(text);
        });
        task.setOnFailed(evt -> aiJobRecommendationArea.setText("AI recommendation failed: " + task.getException().getMessage()));
        new Thread(task, "ai-recommend-jobs").start();
    }

    @FXML
    private void handleAiFillProfileFromCv() {
        if (guestMode) {
            requireLoginAndRedirect("Please sign in first.");
            return;
        }
        Ta ta = session.taOptional().orElse(null);
        if (ta == null) {
            return;
        }
        if (ta.getCvPath() == null || ta.getCvPath().isBlank()) {
            DialogUtil.error("Please upload CV first", navigator.getPrimaryStage());
            return;
        }
        Path cvFile = services.fileStorageHelper().resolveCvFile(ta.getTaId(), ta.getCvPath());
        if (!Files.isRegularFile(cvFile)) {
            DialogUtil.error("CV file not found on disk", navigator.getPrimaryStage());
            return;
        }
        if (aiFillStatusLabel != null) {
            aiFillStatusLabel.setText("AI is filling profile fields...");
        }
        Task<AiService.ResumeDraft> task = new Task<>() {
            @Override
            protected AiService.ResumeDraft call() throws Exception {
                String cvText = Files.readString(cvFile);
                return services.aiService().draftResumeFromCv(ta, cvText);
            }
        };
        task.setOnSucceeded(evt -> {
            AiService.ResumeDraft draft = task.getValue();
            if (!draft.fullName().isBlank()) fullNameField.setText(draft.fullName());
            if (!draft.phone().isBlank()) phoneField.setText(draft.phone());
            if (!draft.major().isBlank()) majorField.setText(draft.major());
            if (!draft.skills().isBlank()) skillsArea.setText(draft.skills());
            if (!draft.experience().isBlank()) experienceArea.setText(draft.experience());
            if (!draft.selfEvaluation().isBlank()) selfEvalArea.setText(draft.selfEvaluation());
            if (aiFillStatusLabel != null) {
                aiFillStatusLabel.setText("Draft generated. It will be saved only after you click Save profile.");
            }
        });
        task.setOnFailed(evt -> {
            if (aiFillStatusLabel != null) {
                aiFillStatusLabel.setText("AI fill failed.");
            }
            DialogUtil.error("AI fill failed: " + task.getException().getMessage(), navigator.getPrimaryStage());
        });
        new Thread(task, "ai-fill-profile").start();
    }

    @FXML
    private void handleAiResumeOptimization() {
        if (guestMode) {
            requireLoginAndRedirect("Please sign in first.");
            return;
        }
        Ta ta = session.taOptional().orElse(null);
        TaJobDisplay display = jobTable.getSelectionModel().getSelectedItem();
        if (ta == null || display == null) {
            DialogUtil.error("Please select a job first", navigator.getPrimaryStage());
            return;
        }
        Job job = display.getJob();
        long contextToken = resumeAdviceContextVersion;
        String jobIdSnapshot = job.getJobId();
        aiResumeAdviceArea.setText("AI is generating suggestions...");
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                if (isCancelled()) {
                    return "";
                }
                return services.aiService().suggestResumeOptimization(ta, job);
            }
        };
        activeResumeAdviceTask = task;
        task.setOnSucceeded(evt -> {
            if (!isResumeAdviceContextValid(contextToken, jobIdSnapshot)) {
                return;
            }
            aiResumeAdviceArea.setText(task.getValue());
        });
        task.setOnCancelled(evt -> {
            if (isResumeAdviceContextValid(contextToken, jobIdSnapshot)) {
                aiResumeAdviceArea.clear();
            }
        });
        task.setOnFailed(evt -> {
            if (!isResumeAdviceContextValid(contextToken, jobIdSnapshot)) {
                return;
            }
            aiResumeAdviceArea.setText("AI suggestion failed: " + task.getException().getMessage());
        });
        new Thread(task, "ai-resume-optimize").start();
    }
}
