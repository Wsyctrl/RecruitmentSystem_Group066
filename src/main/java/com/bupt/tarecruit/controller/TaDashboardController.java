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
import com.bupt.tarecruit.util.CvSaveOutcome;
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
import javafx.stage.Stage;
import javafx.concurrent.Task;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
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
    private Task<List<AiService.JobRecommendation>> activeRecommendJobsTask;
    private final ObservableList<TaJobDisplay> jobItems = FXCollections.observableArrayList();
    private FilteredList<TaJobDisplay> filteredJobs;
    private javafx.collections.transformation.SortedList<TaJobDisplay> sortedJobs;
    /** AI-recommended job ids (lowercased) → score, used to surface and sort AI hits at the top. */
    private final java.util.Map<String, Integer> aiRecommendedJobScores = new java.util.HashMap<>();
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
    private SplitPane browseJobsVerticalSplit;
    @FXML
    private SplitPane browseJobsHorizontalSplit;
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
    private TextArea jobKeywordsArea;
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
    private Button deleteCvButton;
    @FXML
    private Button downloadCvButton;
    @FXML
    private Button uploadCvButton;
    @FXML
    private Button aiFillProfileButton;
    @FXML
    private Label aiFillStatusLabel;
/**
 * Performs controller-specific initialization after shared dependencies
 * have been injected. Sets up job filtering, table bindings, and selection listeners.
 */
    @Override
    protected void onInit() {
        filteredJobs = new FilteredList<>(jobItems, job -> true);
        // Sorted view: AI-recommended jobs first (by score desc), then everything else by jobId.
        sortedJobs = new javafx.collections.transformation.SortedList<>(filteredJobs, this::compareJobsForTable);
        jobTable.setItems(sortedJobs);
        jobTable.setRowFactory(tv -> new javafx.scene.control.TableRow<>() {
            @Override
            protected void updateItem(TaJobDisplay item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().remove("ai-recommended-row");
                getStyleClass().remove("applied-job-row");
                if (empty || item == null) {
                    return;
                }
                if (isApplied(item)) {
                    // Already-applied rows render muted at the bottom.
                    getStyleClass().add("applied-job-row");
                } else if (isAiRecommended(item)) {
                    // AI recommendations only apply to jobs the user hasn't applied for.
                    getStyleClass().add("ai-recommended-row");
                }
            }
        });
        jobTable.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (old != selected) {
                invalidateResumeAdviceContext();
            }
            updateJobDetails(selected);
        });
        // Search only triggers on Refresh button; do not auto-filter on each keystroke.
        applicationTable.setItems(applicationItems);
        applicationTable.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
            // Master-detail behaviour: ensure something stays selected when items exist.
        });
        // Lock split-pane dividers at center.
        if (browseJobsVerticalSplit != null) {
            lockSplitDivider(browseJobsVerticalSplit, 0.62);
        }
        if (browseJobsHorizontalSplit != null) {
            lockSplitDivider(browseJobsHorizontalSplit, 0.5);
        }
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
                if (newTab == myApplicationsTab) {
                    if (applicationTable.getSelectionModel().getSelectedItem() == null
                            && !applicationItems.isEmpty()) {
                        applicationTable.getSelectionModel().selectFirst();
                    }
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

    private static String normalizeJobId(String jobId) {
        return jobId == null ? "" : jobId.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private boolean isAiRecommended(TaJobDisplay display) {
        return display != null
                && display.getJob() != null
                && aiRecommendedJobScores.containsKey(normalizeJobId(display.getJob().getJobId()));
    }

    /**
     * Table ordering:
     *   1) Unapplied AI-recommended jobs first, ordered by AI score (desc, jobId tiebreak).
     *   2) Other unapplied jobs, ordered by jobId ascending.
     *   3) Already-applied jobs at the bottom, ordered by jobId ascending.
     * AI hits never appear in tier 3 because handleAiRecommendJobs excludes applied jobs
     * from the AI pool, but we still treat "applied" as the strongest demotion here.
     */
    private int compareJobsForTable(TaJobDisplay a, TaJobDisplay b) {
        boolean appliedA = isApplied(a);
        boolean appliedB = isApplied(b);
        if (appliedA && !appliedB) return 1;
        if (!appliedA && appliedB) return -1;
        if (!appliedA) {
            boolean aiA = isAiRecommended(a);
            boolean aiB = isAiRecommended(b);
            if (aiA && !aiB) return -1;
            if (!aiA && aiB) return 1;
            if (aiA) {
                int scoreA = aiRecommendedJobScores.getOrDefault(normalizeJobId(a.getJob().getJobId()), 0);
                int scoreB = aiRecommendedJobScores.getOrDefault(normalizeJobId(b.getJob().getJobId()), 0);
                if (scoreA != scoreB) return Integer.compare(scoreB, scoreA);
            }
        }
        return safeText(a.getJob().getJobId()).compareToIgnoreCase(safeText(b.getJob().getJobId()));
    }

    private boolean isApplied(TaJobDisplay display) {
        return display != null
                && display.getJob() != null
                && hasApplied(display.getJob().getJobId());
    }

    private void refreshJobTableOrder() {
        if (sortedJobs != null) {
            sortedJobs.setComparator(null);
            sortedJobs.setComparator(this::compareJobsForTable);
        }
        if (jobTable != null) {
            jobTable.refresh();
        }
    }

    private void lockSplitDivider(SplitPane split, double position) {
        if (split == null) return;
        split.setDividerPositions(position);
        split.getDividers().forEach(d -> d.positionProperty().addListener((obs, oldV, newV) -> {
            if (Math.abs(newV.doubleValue() - position) > 0.0001) {
                javafx.application.Platform.runLater(() -> d.setPosition(position));
            }
        }));
        javafx.application.Platform.runLater(() ->
                split.lookupAll(".split-pane-divider").forEach(node -> node.setMouseTransparent(true)));
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
        // Drop jobs posted by a disabled MO: they cannot accept new applications.
        jobs = jobs.stream()
                .filter(job -> services.profileService().findMo(job.getMoId())
                        .map(mo -> !mo.isDisabled())
                        .orElse(true))
                .collect(Collectors.toList());
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
        // A fresh load invalidates any previous AI recommendation highlighting and ordering.
        aiRecommendedJobScores.clear();
        refreshJobTableOrder();
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
        if (!applicationItems.isEmpty()
                && applicationTable.getSelectionModel().getSelectedItem() == null) {
            applicationTable.getSelectionModel().selectFirst();
        }
        // Applied-status changes affect the Browse jobs ordering and styling.
        refreshJobTableOrder();
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
    }
/**
 * Updates the CV-related UI controls according to whether
 * the current TA user has uploaded a CV file.
 *
 * @param ta current TA user
 */
    private void updateCvUi(Ta ta) {
        boolean hasCv = ta.getCvPath() != null && !ta.getCvPath().isBlank();
        if (cvPathLabel != null) {
            if (hasCv) {
                cvPathLabel.setText("Uploaded");
                if (!cvPathLabel.getStyleClass().contains("cv-file-uploaded")) {
                    cvPathLabel.getStyleClass().add("cv-file-uploaded");
                }
            } else {
                cvPathLabel.setText("No file uploaded");
                cvPathLabel.getStyleClass().remove("cv-file-uploaded");
            }
        }
        if (uploadCvButton != null) {
            uploadCvButton.setText(hasCv ? "Re-upload CV" : "Upload CV");
        }
        if (deleteCvButton != null) {
            deleteCvButton.setDisable(!hasCv);
        }
        if (downloadCvButton != null) {
            downloadCvButton.setDisable(!hasCv);
        }
        if (aiFillProfileButton != null) {
            aiFillProfileButton.setDisable(!hasCv);
        }
    }
/**
 * Applies a keyword filter to the visible job list.
 * Search covers all job-related fields the TA can see in the table or detail panel.
 *
 * @param keyword search keyword entered by the user
 */
    private void applyJobFilter(String keyword) {
        if (filteredJobs == null) {
            return;
        }
        String lower = keyword == null ? "" : keyword.trim().toLowerCase();
        if (lower.isEmpty()) {
            filteredJobs.setPredicate(display -> true);
            return;
        }
        filteredJobs.setPredicate(display -> {
            Job job = display.getJob();
            return containsIgnoreCase(job.getJobId(), lower)
                    || containsIgnoreCase(job.getJobName(), lower)
                    || containsIgnoreCase(job.getModuleName(), lower)
                    || containsIgnoreCase(job.getMoName(), lower)
                    || containsIgnoreCase(job.getMoId(), lower)
                    || containsIgnoreCase(job.getRequirements(), lower)
                    || containsIgnoreCase(job.getAdditionalNotes(), lower)
                    || containsIgnoreCase(job.getKeywords(), lower);
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
            if (jobKeywordsArea != null) {
                jobKeywordsArea.clear();
            }
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
        if (jobKeywordsArea != null) {
            jobKeywordsArea.setText(safeText(job.getKeywords()));
        }
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
            // Keep focus on the job table so the cursor doesn't drift to the AI preference area.
            javafx.application.Platform.runLater(jobTable::requestFocus);
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
    private void handleChangePasswordFromProfile() {
        if (guestMode) {
            requireLoginAndRedirect("Please sign in first.");
            return;
        }
        Ta ta = session.taOptional().orElse(null);
        if (ta == null) {
            return;
        }

        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/fxml/change-password-dialog.fxml"));
            javafx.scene.Parent root = loader.load();

            ChangePasswordDialogController controller = loader.getController();
            controller.setServices(services);

            // Create dialog stage
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Change Password");
            dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialogStage.initOwner(navigator.getPrimaryStage());
            dialogStage.setResizable(false);

            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.getStylesheets().add(
                    getClass().getResource("/css/application.css").toExternalForm());
            dialogStage.setScene(scene);

            controller.setDialogStage(dialogStage);
            controller.setTaId(ta.getTaId());

            dialogStage.showAndWait();

            // If password was changed, show success message
            if (controller.isPasswordChanged()) {
                DialogUtil.info("Password changed successfully!", navigator.getPrimaryStage());
            }
        } catch (Exception e) {
            DialogUtil.error("Failed to open change password dialog: " + e.getMessage(), navigator.getPrimaryStage());
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
        CvSaveOutcome outcome = helper.saveCv(ta.getTaId(), selected);
        ta.setCvPath(outcome.relativePath());
        services.profileService().updateTa(ta, outcome.contentChanged());
        updateCvUi(ta);
        DialogUtil.info("CV uploaded", navigator.getPrimaryStage());
    }

    @FXML
    private void handleDeleteCv() {
        if (guestMode) {
            requireLoginAndRedirect("Please sign in first.");
            return;
        }
        Ta ta = session.taOptional().orElse(null);
        if (ta == null) {
            return;
        }
        if (!DialogUtil.confirmYesNo("Delete your uploaded resume attachment?", navigator.getPrimaryStage())) {
            return;
        }
        try {
            services.fileStorageHelper().deleteCv(ta.getTaId(), ta.getCvPath());
        } catch (IOException e) {
            DialogUtil.error("Failed to delete CV: " + e.getMessage(), navigator.getPrimaryStage());
            return;
        }
        ta.setCvPath("");
        services.profileService().updateTa(ta);
        updateCvUi(ta);
        if (aiFillStatusLabel != null) {
            aiFillStatusLabel.setText("");
        }
        DialogUtil.info("CV deleted", navigator.getPrimaryStage());
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

        Path source = services.fileStorageHelper().resolveCvFile(ta.getTaId(), ta.getCvPath());
        if (!Files.isRegularFile(source)) {
            DialogUtil.error("CV file not found on disk", navigator.getPrimaryStage());
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialFileName(FileStorageHelper.cvFileName(ta.getTaId()));
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
        applyJobFilter(jobSearchField == null ? "" : jobSearchField.getText());
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
    private void handleResetAiJobRecommendations() {
        if (activeRecommendJobsTask != null && activeRecommendJobsTask.isRunning()) {
            activeRecommendJobsTask.cancel(true);
        }
        aiRecommendedJobScores.clear();
        clearAiJobRecommendationOutputs();
        refreshJobTableOrder();
    }

    private void clearAiJobRecommendationOutputs() {
        if (aiJobRecommendationArea != null) {
            aiJobRecommendationArea.clear();
        }
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
        // AI pool: open jobs from active MOs that the TA hasn't already applied for.
        // Disabled MOs and applied jobs are filtered out so they can't be ranked or highlighted.
        List<Job> jobs = services.jobService().findOpenJobs().stream()
                .filter(job -> services.profileService().findMo(job.getMoId())
                        .map(mo -> !mo.isDisabled())
                        .orElse(true))
                .filter(job -> !hasApplied(job.getJobId()))
                .collect(Collectors.toList());
        if (jobs.isEmpty()) {
            aiRecommendedJobScores.clear();
            refreshJobTableOrder();
            aiJobRecommendationArea.setText(
                    "No open jobs are available to recommend (you may have applied to all eligible postings).");
            return;
        }
        aiJobRecommendationArea.setText("AI is analyzing...");
        String preference = aiJobPreferenceField == null ? "" : aiJobPreferenceField.getText();
        // The AI service is asked for "the best 3"; we'll cap to whatever the eligible pool allows.
        final int targetTopN = Math.min(3, jobs.size());
        final String cvText = readAttachedCvText(ta);
        Task<List<AiService.JobRecommendation>> task = new Task<>() {
            @Override
            protected List<AiService.JobRecommendation> call() throws Exception {
                return services.aiService().recommendJobsForTa(ta, jobs, preference, cvText);
            }
        };
        task.setOnSucceeded(evt -> {
            if (task.isCancelled()) {
                return;
            }
            List<AiService.JobRecommendation> items = task.getValue();
            // Reset previous highlights so stale recommendations don't persist.
            aiRecommendedJobScores.clear();

            // Map AI ids (case-insensitive) onto the eligible pool only.
            java.util.Map<String, Job> jobByNormalizedId = new java.util.HashMap<>();
            for (Job j : jobs) {
                jobByNormalizedId.put(normalizeJobId(j.getJobId()), j);
            }
            java.util.LinkedHashMap<String, AiService.JobRecommendation> validRecs = new java.util.LinkedHashMap<>();
            if (items != null) {
                for (AiService.JobRecommendation rec : items) {
                    String key = normalizeJobId(rec.jobId());
                    if (jobByNormalizedId.containsKey(key) && !validRecs.containsKey(key)) {
                        validRecs.put(key, rec);
                        aiRecommendedJobScores.put(key, rec.score());
                    }
                }
            }

            // Eligible pool < 3 (or AI returned fewer hits than the cap): pad up to targetTopN
            // by walking the remaining eligible jobs in jobId order with score 0 so they still
            // surface as TOP MATCH rather than dropping back to the unhighlighted block.
            if (aiRecommendedJobScores.size() < targetTopN) {
                List<Job> sortedPool = new java.util.ArrayList<>(jobs);
                sortedPool.sort(Comparator.comparing(j -> safeText(j.getJobId()), String.CASE_INSENSITIVE_ORDER));
                for (Job j : sortedPool) {
                    if (aiRecommendedJobScores.size() >= targetTopN) break;
                    String key = normalizeJobId(j.getJobId());
                    aiRecommendedJobScores.putIfAbsent(key, 0);
                }
            }

            refreshJobTableOrder();

            if (aiRecommendedJobScores.isEmpty()) {
                aiJobRecommendationArea.setText("No AI recommendations matched the visible job list.");
                return;
            }
            StringBuilder sb = new StringBuilder("Top " + aiRecommendedJobScores.size() + " match(es):\n\n");
            int idx = 1;
            for (AiService.JobRecommendation item : validRecs.values()) {
                if (idx > targetTopN) break;
                Job job = jobByNormalizedId.get(normalizeJobId(item.jobId()));
                String title = job == null ? item.jobId() : job.getJobName() + " (" + item.jobId() + ")";
                sb.append(idx++).append(". ").append(title)
                        .append(" | Match Score: ").append(item.score())
                        .append("\n   ").append(item.reason()).append("\n\n");
            }
            if (validRecs.size() < targetTopN) {
                sb.append("(AI returned only ").append(validRecs.size())
                        .append(" matches; remaining slots filled from the rest of the eligible pool.)\n");
            }
            aiJobRecommendationArea.setText(sb.toString());
        });
        task.setOnFailed(evt -> {
            if (!task.isCancelled()) {
                aiJobRecommendationArea.setText("AI recommendation failed: " + task.getException().getMessage());
            }
        });
        if (activeRecommendJobsTask != null && activeRecommendJobsTask.isRunning()) {
            activeRecommendJobsTask.cancel(true);
        }
        activeRecommendJobsTask = task;
        new Thread(task, "ai-recommend-jobs").start();
    }

    private String readAttachedCvText(Ta ta) {
        if (ta.getCvPath() == null || ta.getCvPath().isBlank()) {
            return "";
        }
        Path cvFile = services.fileStorageHelper().resolveCvFile(ta.getTaId(), ta.getCvPath());
        if (!Files.isRegularFile(cvFile)) {
            return "";
        }
        try {
            return Files.readString(cvFile);
        } catch (IOException e) {
            return "";
        }
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
