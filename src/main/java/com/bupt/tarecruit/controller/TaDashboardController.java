package com.bupt.tarecruit.controller;

import com.bupt.tarecruit.entity.Job;
import com.bupt.tarecruit.entity.Mo;
import com.bupt.tarecruit.entity.Ta;
import com.bupt.tarecruit.entity.UserSession;
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

    private UserSession session;
    private final ObservableList<TaJobDisplay> jobItems = FXCollections.observableArrayList();
    private FilteredList<TaJobDisplay> filteredJobs;
    private final ObservableList<ApplicationDisplay> applicationItems = FXCollections.observableArrayList();

    @FXML
    private Label welcomeLabel;
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
        jobTable.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> updateJobDetails(selected));
        jobSearchField.textProperty().addListener((obs, old, value) -> applyJobFilter(value));
        applicationTable.setItems(applicationItems);
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
        welcomeLabel.setText("Welcome, " + session.getDisplayName());
        loadInitialData();
    }
/**
 * Loads all initial dashboard data, including jobs,
 * existing applications, and profile information.
 */
    private void loadInitialData() {
        refreshJobs();
        refreshApplications();
        loadProfile();
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
        String taId = session.taOptional().map(Ta::getTaId).orElse("");
        ApplicationService applicationService = services.applicationService();
        Map<String, Job> jobMap = services.jobService().findAllJobs().stream()
                .collect(Collectors.toMap(Job::getJobId, Function.identity(), (a, b) -> a));
        applicationItems.setAll(applicationService.findActiveApplicationsForTa(taId).stream()
                .map(record -> new ApplicationDisplay(record, jobMap.get(record.getJobId())))
                .collect(Collectors.toList()));
        applicationTable.refresh();
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
            return;
        }
        Job job = display.getJob();
        jobNameLabel.setText(job.getJobName());
        jobMoNameLabel.setText(job.getMoName() == null || job.getMoName().isBlank() ? job.getMoId() : job.getMoName());
        jobModuleLabel.setText(job.getModuleName());
        jobPositionsLabel.setText("Positions: " + job.getNumberOfPositions());
        String start = formatDateOrPlaceholder(DateTimeUtil.formatDate(job.getStartDate()));
        String end = formatDateOrPlaceholder(DateTimeUtil.formatDate(job.getEndDate()));
        jobDateLabel.setText(start + " to " + end);
        jobRequirementsArea.setText(safeText(job.getRequirements()));
        jobNotesArea.setText(safeText(job.getAdditionalNotes()));
        applyButton.setDisable(!job.isOpen() || hasApplied(job.getJobId()));
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
        Ta ta = session.taOptional().orElse(null);
        if (ta == null) {
            return;
        }
        ta.setFullName(fullNameField.getText() == null ? "" : fullNameField.getText().trim());
        ta.setPhone(phoneField.getText());
        ta.setEmail(emailField.getText());
        ta.setMajor(majorField.getText());
        ta.setSkills(skillsArea.getText());
        ta.setExperience(experienceArea.getText());
        ta.setSelfEvaluation(selfEvalArea.getText());
        OperationResult<Ta> result = services.profileService().updateTa(ta);
        if (result.success()) {
            welcomeLabel.setText("Welcome, " + session.getDisplayName());
            DialogUtil.info(result.message(), navigator.getPrimaryStage());
        } else {
            DialogUtil.error(result.message(), navigator.getPrimaryStage());
        }
    }

    @FXML
    private void handleChangePassword() {
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
    private void handleLogout() {
        navigator.showLogin();
    }

    @FXML
    private void handleRefreshJobs() {
        refreshJobs();
    }
}
