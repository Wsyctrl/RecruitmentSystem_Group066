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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TaDashboardController extends BaseController implements SessionAware {

    private UserSession session;
    private final ObservableList<Job> jobItems = FXCollections.observableArrayList();
    private FilteredList<Job> filteredJobs;
    private final ObservableList<ApplicationDisplay> applicationItems = FXCollections.observableArrayList();

    @FXML
    private Label welcomeLabel;
    @FXML
    private TextField jobSearchField;
    @FXML
    private TableView<Job> jobTable;
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

    @Override
    protected void onInit() {
        filteredJobs = new FilteredList<>(jobItems, job -> true);
        jobTable.setItems(filteredJobs);
        jobTable.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> updateJobDetails(selected));
        jobSearchField.textProperty().addListener((obs, old, value) -> applyJobFilter(value));
        applicationTable.setItems(applicationItems);
        updateJobDetails(null);
    }

    @Override
    public void setSession(UserSession session) {
        this.session = session;
        welcomeLabel.setText("Welcome, " + session.getDisplayName());
        loadInitialData();
    }

    private void loadInitialData() {
        refreshJobs();
        refreshApplications();
        loadProfile();
    }

    private void refreshJobs() {
        JobService jobService = services.jobService();
        jobItems.setAll(jobService.findOpenJobs());
        // Fill MO display names for UI rendering.
        for (Job job : jobItems) {
            String moId = job.getMoId();
            services.profileService().findMo(moId)
                    .map(Mo::getDisplayLabel)
                    .ifPresent(job::setMoName);
        }
        if (!jobItems.isEmpty()) {
            jobTable.getSelectionModel().selectFirst();
        } else {
            updateJobDetails(null);
        }
    }

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

    private void updateCvUi(Ta ta) {
        boolean hasCv = ta.getCvPath() != null && !ta.getCvPath().isBlank();
        cvPathLabel.setText(hasCv ? "Uploaded" : "None");
        if (downloadCvButton != null) {
            downloadCvButton.setVisible(hasCv);
            downloadCvButton.setManaged(hasCv);
        }
    }

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

    private void applyJobFilter(String keyword) {
        if (filteredJobs == null) {
            return;
        }
        String lower = keyword == null ? "" : keyword.toLowerCase();
        filteredJobs.setPredicate(job -> containsIgnoreCase(job.getJobName(), lower)
                || containsIgnoreCase(job.getModuleName(), lower)
                || containsIgnoreCase(job.getRequirements(), lower));
    }

    private boolean containsIgnoreCase(String source, String keyword) {
        if (source == null) {
            return false;
        }
        return source.toLowerCase().contains(keyword);
    }

    private String formatDateOrPlaceholder(String value) {
        return (value == null || value.isBlank()) ? "-" : value;
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private void updateJobDetails(Job job) {
        if (job == null) {
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

    private boolean hasApplied(String jobId) {
        return applicationItems.stream().anyMatch(display -> display.getRecord().getJobId().equalsIgnoreCase(jobId));
    }

    @FXML
    private void handleApply() {
        Job job = jobTable.getSelectionModel().getSelectedItem();
        if (job == null) {
            DialogUtil.error("Please select a job first", navigator.getPrimaryStage());
            return;
        }
        String taId = session.taOptional().map(Ta::getTaId).orElse("");
        OperationResult<?> result = services.applicationService().applyForJob(taId, job);
        if (result.success()) {
            DialogUtil.info(result.message(), navigator.getPrimaryStage());
            refreshApplications();
            updateJobDetails(job);
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
