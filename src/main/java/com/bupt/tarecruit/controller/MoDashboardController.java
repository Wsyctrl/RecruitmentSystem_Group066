package com.bupt.tarecruit.controller;

import com.bupt.tarecruit.entity.*;
import com.bupt.tarecruit.service.ApplicationService;
import com.bupt.tarecruit.util.DateTimeUtil;
import com.bupt.tarecruit.util.DialogUtil;
import com.bupt.tarecruit.util.OperationResult;
import com.bupt.tarecruit.viewmodel.ApplicantDisplay;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.SpinnerValueFactory;
import javafx.stage.FileChooser;

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

public class MoDashboardController extends BaseController implements SessionAware {

    private UserSession session;
    private boolean adminMode;
    private Job currentEditingJob;

    private final ObservableList<Job> myJobs = FXCollections.observableArrayList();
    private FilteredList<Job> filteredMyJobs;
    private final ObservableList<Job> jobOptions = FXCollections.observableArrayList();
    private final ObservableList<ApplicantDisplay> applicantItems = FXCollections.observableArrayList();
    private FilteredList<ApplicantDisplay> filteredApplicants;
    private final ObservableList<Ta> taUsers = FXCollections.observableArrayList();
    private final ObservableList<Mo> moUsers = FXCollections.observableArrayList();
    private final ObservableList<Job> adminJobItems = FXCollections.observableArrayList();

    @FXML
    private TabPane tabPane;
    @FXML
    private Label welcomeLabel;
    @FXML
    private TextField myJobSearchField;
    @FXML
    private TableView<Job> myJobTable;
    @FXML
    private Label selectedJobNameLabel;
    @FXML
    private Label selectedJobDatesLabel;
    @FXML
    private Label selectedJobApplicantsLabel;
    @FXML
    private TextArea selectedJobRequirementsArea;
    @FXML
    private TextArea selectedJobNotesArea;

    @FXML
    private TextField jobNameField;
    @FXML
    private TextField moduleField;
    @FXML
    private Spinner<Integer> positionsSpinner;
    @FXML
    private DatePicker startDatePicker;
    @FXML
    private DatePicker endDatePicker;
    @FXML
    private TextArea requirementsField;
    @FXML
    private TextArea notesField;
    @FXML
    private Label formStatusLabel;
    @FXML
    private Label formJobIdLabel;

    @FXML
    private ComboBox<Job> jobSelector;
    @FXML
    private TextField applicantSearchField;
    @FXML
    private TableView<ApplicantDisplay> applicantTable;
    @FXML
    private Label applicantNameLabel;
    @FXML
    private Label applicantStatusLabel;
    @FXML
    private TextArea applicantProfileArea;

    @FXML
    private TextField moFullNameField;
    @FXML
    private TextField moPhoneField;
    @FXML
    private TextField moEmailField;
    @FXML
    private TextArea moModuleArea;
    @FXML
    private PasswordField moCurrentPasswordField;
    @FXML
    private PasswordField moNewPasswordField;
    @FXML
    private PasswordField moConfirmPasswordField;

    @FXML
    private Tab adminUserTab;
    @FXML
    private Tab adminJobTab;
    @FXML
    private TableView<Ta> taUserTable;
    @FXML
    private TableView<Mo> moUserTable;
    @FXML
    private TableView<Job> adminJobTable;

    @Override
    protected void onInit() {
        filteredMyJobs = new FilteredList<>(myJobs, job -> true);
        myJobTable.setItems(filteredMyJobs);
        myJobSearchField.textProperty().addListener((obs, old, val) -> filterJobs(val));
        myJobTable.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> updateSelectedJob(val));

        jobSelector.setItems(jobOptions);
        jobSelector.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Job item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getJobName());
            }
        });
        jobSelector.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Job item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getJobName());
            }
        });
        jobSelector.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> loadApplicants(val));

        filteredApplicants = new FilteredList<>(applicantItems, item -> true);
        applicantTable.setItems(filteredApplicants);
        applicantSearchField.textProperty().addListener((obs, old, val) -> filterApplicants(val));
        applicantTable.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> updateApplicantDetail(val));

        positionsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 50, 1));
        setupAdminVisibility(false);

        // When switching to "All jobs", always refresh so new postings appear immediately.
        if (tabPane != null) {
            tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
                if (newTab != null && newTab == adminJobTab) {
                    refreshAdminJobs();
                }
            });
        }
    }

    private void refreshAdminJobs() {
        if (!adminMode) {
            return;
        }
        if (adminJobItems == null) {
            return;
        }
        adminJobItems.setAll(services.jobService().findAllJobs());
        if (adminJobTable != null) {
            adminJobTable.refresh();
        }
    }

    private void setupAdminVisibility(boolean enabled) {
        if (tabPane == null) {
            return;
        }
        if (enabled) {
            if (!tabPane.getTabs().contains(adminUserTab)) {
                tabPane.getTabs().add(adminUserTab);
            }
            if (!tabPane.getTabs().contains(adminJobTab)) {
                tabPane.getTabs().add(adminJobTab);
            }
        } else {
            tabPane.getTabs().remove(adminUserTab);
            tabPane.getTabs().remove(adminJobTab);
        }
    }

    @Override
    public void setSession(UserSession session) {
        this.session = session;
        this.adminMode = session.role() == Role.ADMIN;
        welcomeLabel.setText("Welcome, " + session.getDisplayName());
        setupAdminVisibility(adminMode);
        if (adminMode && tabPane != null && adminUserTab != null) {
            tabPane.getSelectionModel().select(adminUserTab);
        }
        loadData();
    }

    private void loadData() {
        refreshMyJobs();
        loadProfile();
        if (adminMode) {
            loadAdminData();
        }
    }

    private String currentMoId() {
        return session.moOptional().map(Mo::getMoId).orElse("");
    }

    private void refreshMyJobs() {
        List<Job> jobs = services.jobService().findOpenJobsByMo(currentMoId());
        jobs.sort(Comparator.comparing(Job::getJobId));
        myJobs.setAll(jobs);
        jobOptions.setAll(jobs);
        if (!jobs.isEmpty()) {
            myJobTable.getSelectionModel().selectFirst();
            jobSelector.getSelectionModel().selectFirst();
        } else {
            updateSelectedJob(null);
        }
    }

    private void updateSelectedJob(Job job) {
        if (job == null) {
            selectedJobNameLabel.setText("None selected");
            selectedJobDatesLabel.setText("-");
            selectedJobApplicantsLabel.setText("Applicants: 0");
            selectedJobRequirementsArea.clear();
            selectedJobNotesArea.clear();
            return;
        }
        selectedJobNameLabel.setText(job.getJobName());
        selectedJobDatesLabel.setText(DateTimeUtil.formatDate(job.getStartDate()) + " to " + DateTimeUtil.formatDate(job.getEndDate()));
        int count = services.applicationService().findActiveApplicationsForJob(job.getJobId()).size();
        selectedJobApplicantsLabel.setText("Applicants: " + count);
        selectedJobRequirementsArea.setText(safeText(job.getRequirements()));
        selectedJobNotesArea.setText(safeText(job.getAdditionalNotes()));
    }

    private void filterJobs(String keyword) {
        String lower = keyword == null ? "" : keyword.toLowerCase();
        filteredMyJobs.setPredicate(job -> {
            String name = job.getJobName() == null ? "" : job.getJobName().toLowerCase();
            String module = job.getModuleName() == null ? "" : job.getModuleName().toLowerCase();
            return name.contains(lower) || module.contains(lower);
        });
    }

    private void loadApplicants(Job job) {
        if (job == null) {
            applicantItems.clear();
            return;
        }
        List<Ta> allTa = services.adminService().findAllTa();
        Map<String, Ta> taMap = allTa.stream().collect(Collectors.toMap(Ta::getTaId, Function.identity(), (a, b) -> a));
        ApplicationService applicationService = services.applicationService();
        applicantItems.setAll(applicationService.findActiveApplicationsForJob(job.getJobId()).stream()
                .map(record -> new ApplicantDisplay(record, taMap.get(record.getTaId())))
                .collect(Collectors.toList()));
        filteredApplicants.setPredicate(item -> true);
        if (!applicantItems.isEmpty()) {
            applicantTable.getSelectionModel().selectFirst();
        } else {
            updateApplicantDetail(null);
        }
    }

    private void filterApplicants(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            filteredApplicants.setPredicate(item -> true);
            return;
        }
        String lower = keyword.toLowerCase();
        String raw = keyword.trim();
        filteredApplicants.setPredicate(applicant -> applicant.getTaId().toLowerCase().contains(lower)
                || applicant.getTaName().contains(raw)
                || (applicant.getPhone() != null && applicant.getPhone().toLowerCase().contains(lower))
                || (applicant.getEmail() != null && applicant.getEmail().toLowerCase().contains(lower))
                || applicant.getStatus().contains(raw));
    }

    private void updateApplicantDetail(ApplicantDisplay display) {
        if (display == null || display.getTa() == null) {
            applicantNameLabel.setText("None selected");
            applicantStatusLabel.setText("-");
            applicantProfileArea.clear();
            return;
        }
        Ta ta = display.getTa();
        applicantNameLabel.setText(ta.getDisplayLabel() + "  (" + ta.getTaId() + ")");
        applicantStatusLabel.setText(display.getStatus());
        applicantProfileArea.setText("""
Name: %s
User ID: %s
Phone: %s
Email: %s
Major: %s
Skills: %s
Experience: %s
Self-eval: %s
CV: %s
""".formatted(
                ta.getDisplayLabel(),
                ta.getTaId(),
                safeText(ta.getPhone()),
                safeText(ta.getEmail()),
                safeText(ta.getMajor()),
                safeText(ta.getSkills()),
                safeText(ta.getExperience()),
                safeText(ta.getSelfEvaluation()),
                ta.getCvPath() != null && !ta.getCvPath().isBlank() ? "Uploaded" : "None"
        ));
    }

    private void loadProfile() {
        Mo mo = session.moOptional().orElse(null);
        if (mo == null) {
            return;
        }
        moFullNameField.setText(mo.getFullName() == null ? "" : mo.getFullName());
        moPhoneField.setText(mo.getPhone());
        moEmailField.setText(mo.getEmail());
        moModuleArea.setText(mo.getResponsibleModules());
        if (moCurrentPasswordField != null) {
            moCurrentPasswordField.clear();
        }
        if (moNewPasswordField != null) {
            moNewPasswordField.clear();
        }
        if (moConfirmPasswordField != null) {
            moConfirmPasswordField.clear();
        }
    }

    private void loadAdminData() {
        taUsers.setAll(services.adminService().findAllTa());
        moUsers.setAll(services.adminService().findAllMo().stream()
                .filter(mo -> !mo.isAdmin())
                .collect(Collectors.toList()));
        adminJobItems.setAll(services.jobService().findAllJobs());
        taUserTable.setItems(taUsers);
        moUserTable.setItems(moUsers);
        adminJobTable.setItems(adminJobItems);
    }

    @FXML
    private void handleRefreshMyJobs() {
        refreshMyJobs();
    }

    @FXML
    private void handleEditFromSelected() {
        Job selected = myJobTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            DialogUtil.error("Please select a job to edit", navigator.getPrimaryStage());
            return;
        }
        populateJobForm(selected);
        tabPane.getSelectionModel().select(1);
    }

    @FXML
    private void handleCreateNewJob() {
        currentEditingJob = null;
        jobNameField.clear();
        moduleField.clear();
        positionsSpinner.getValueFactory().setValue(1);
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
        requirementsField.clear();
        notesField.clear();
        formJobIdLabel.setText("New job");
        formStatusLabel.setText("");
    }

    private void populateJobForm(Job job) {
        currentEditingJob = job;
        jobNameField.setText(job.getJobName());
        moduleField.setText(job.getModuleName());
        positionsSpinner.getValueFactory().setValue(job.getNumberOfPositions());
        startDatePicker.setValue(job.getStartDate());
        endDatePicker.setValue(job.getEndDate());
        requirementsField.setText(safeText(job.getRequirements()));
        notesField.setText(safeText(job.getAdditionalNotes()));
        formJobIdLabel.setText("Edit: " + job.getJobId());
    }

    @FXML
    private void handleSaveJob() {
        try {
            Job job = currentEditingJob == null ? new Job() : currentEditingJob;
            job.setJobName(jobNameField.getText());
            job.setModuleName(moduleField.getText());
            job.setNumberOfPositions(positionsSpinner.getValue());
            job.setStartDate(startDatePicker.getValue());
            job.setEndDate(endDatePicker.getValue());
            job.setRequirements(requirementsField.getText());
            job.setAdditionalNotes(notesField.getText());
            job.setMoId(currentMoId());
            OperationResult<Job> result = services.jobService().upsertJob(job);
            if (result.success()) {
                currentEditingJob = result.data();
                formStatusLabel.setText(result.message());
                refreshMyJobs();
                if (adminMode) {
                    refreshAdminJobs();
                }
            } else {
                DialogUtil.error(result.message(), navigator.getPrimaryStage());
            }
        } catch (IllegalArgumentException ex) {
            DialogUtil.error(ex.getMessage(), navigator.getPrimaryStage());
        }
    }

    @FXML
    private void handleCloseJob() {
        Job job = myJobTable.getSelectionModel().getSelectedItem();
        if (job == null) {
            DialogUtil.error("Please select a job", navigator.getPrimaryStage());
            return;
        }
        if (DialogUtil.confirm("Close this job?", navigator.getPrimaryStage())) {
            OperationResult<Void> result = services.jobService().closeJob(job.getJobId());
            if (result.success()) {
                services.applicationService().rejectPendingApplicationsForJob(job.getJobId());
                refreshMyJobs();
            } else {
                DialogUtil.error(result.message(), navigator.getPrimaryStage());
            }
        }
    }

    @FXML
    private void handleJumpToApplicants() {
        Job job = myJobTable.getSelectionModel().getSelectedItem();
        if (job != null) {
            jobSelector.getSelectionModel().select(job);
            loadApplicants(job);
            tabPane.getSelectionModel().select(2);
        }
    }

    @FXML
    private void handleHireApplicant() {
        ApplicantDisplay display = applicantTable.getSelectionModel().getSelectedItem();
        if (display == null) {
            DialogUtil.error("Please select an applicant", navigator.getPrimaryStage());
            return;
        }
        if (DialogUtil.confirm("Hire this applicant?", navigator.getPrimaryStage())) {
            OperationResult<Void> result = services.applicationService().hireApplicant(display.getRecord().getApplyId());
            if (result.success()) {
                DialogUtil.info(result.message(), navigator.getPrimaryStage());
                refreshMyJobs();
                loadApplicants(jobSelector.getSelectionModel().getSelectedItem());
            } else {
                DialogUtil.error(result.message(), navigator.getPrimaryStage());
            }
        }
    }

    @FXML
    private void handleRejectApplicant() {
        ApplicantDisplay display = applicantTable.getSelectionModel().getSelectedItem();
        if (display == null) {
            DialogUtil.error("Please select an applicant", navigator.getPrimaryStage());
            return;
        }
        OperationResult<Void> result = services.applicationService().rejectApplicant(display.getRecord().getApplyId());
        if (result.success()) {
            DialogUtil.info(result.message(), navigator.getPrimaryStage());
            loadApplicants(jobSelector.getSelectionModel().getSelectedItem());
        } else {
            DialogUtil.error(result.message(), navigator.getPrimaryStage());
        }
    }

    @FXML
    private void handleDownloadCv() {
        ApplicantDisplay display = applicantTable.getSelectionModel().getSelectedItem();
        if (display == null || display.getTa() == null) {
            DialogUtil.error("Please select an applicant first", navigator.getPrimaryStage());
            return;
        }
        Ta ta = display.getTa();
        Path source = services.fileStorageHelper().resolveCvFile(ta.getTaId(), ta.getCvPath());
        if (!Files.isRegularFile(source)) {
            DialogUtil.error("CV file not found. The applicant may not have uploaded a CV yet.", navigator.getPrimaryStage());
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
        } catch (IOException e) {
            DialogUtil.error("Download failed: " + e.getMessage(), navigator.getPrimaryStage());
        }
    }

    @FXML
    private void handleRefreshApplicants() {
        loadApplicants(jobSelector.getSelectionModel().getSelectedItem());
    }

    @FXML
    private void handleSaveMoProfile() {
        Mo mo = session.moOptional().orElse(null);
        if (mo == null) {
            return;
        }
        mo.setFullName(moFullNameField.getText() == null ? "" : moFullNameField.getText().trim());
        mo.setPhone(moPhoneField.getText());
        mo.setEmail(moEmailField.getText());
        mo.setResponsibleModules(moModuleArea.getText());
        OperationResult<Mo> result = services.profileService().updateMo(mo);
        if (result.success()) {
            welcomeLabel.setText("Welcome, " + session.getDisplayName());
            DialogUtil.info(result.message(), navigator.getPrimaryStage());
        } else {
            DialogUtil.error(result.message(), navigator.getPrimaryStage());
        }
    }

    @FXML
    private void handleChangeMoPassword() {
        Mo mo = session.moOptional().orElse(null);
        if (mo == null) {
            return;
        }
        String cur = moCurrentPasswordField.getText() == null ? "" : moCurrentPasswordField.getText();
        String nw = moNewPasswordField.getText() == null ? "" : moNewPasswordField.getText();
        String cf = moConfirmPasswordField.getText() == null ? "" : moConfirmPasswordField.getText();
        OperationResult<Void> result = services.profileService()
                .changeMoPassword(mo.getMoId(), cur, nw, cf);
        if (result.success()) {
            mo.setPassword(nw);
            moCurrentPasswordField.clear();
            moNewPasswordField.clear();
            moConfirmPasswordField.clear();
            DialogUtil.info(result.message(), navigator.getPrimaryStage());
        } else {
            DialogUtil.error(result.message(), navigator.getPrimaryStage());
        }
    }

    @FXML
    private void handleLogout() {
        navigator.showLogin();
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    // Admin operations
    @FXML
    private void handleResetTaPassword() {
        Ta ta = taUserTable.getSelectionModel().getSelectedItem();
        if (ta == null) {
            DialogUtil.error("Please select a TA user", navigator.getPrimaryStage());
            return;
        }
        OperationResult<Void> result = services.adminService().resetPassword(Role.TA, ta.getTaId());
        DialogUtil.info(result.message(), navigator.getPrimaryStage());
        loadAdminData();
    }

    @FXML
    private void handleToggleTaStatus() {
        Ta ta = taUserTable.getSelectionModel().getSelectedItem();
        if (ta == null) {
            DialogUtil.error("Please select a TA user", navigator.getPrimaryStage());
            return;
        }
        OperationResult<Void> result = services.adminService().toggleStatus(Role.TA, ta.getTaId(), !ta.isDisabled());
        DialogUtil.info(result.message(), navigator.getPrimaryStage());
        loadAdminData();
    }

    @FXML
    private void handleResetMoPassword() {
        Mo mo = moUserTable.getSelectionModel().getSelectedItem();
        if (mo == null) {
            DialogUtil.error("Please select an MO user", navigator.getPrimaryStage());
            return;
        }
        OperationResult<Void> result = services.adminService().resetPassword(Role.MO, mo.getMoId());
        DialogUtil.info(result.message(), navigator.getPrimaryStage());
        loadAdminData();
    }

    @FXML
    private void handleToggleMoStatus() {
        Mo mo = moUserTable.getSelectionModel().getSelectedItem();
        if (mo == null) {
            DialogUtil.error("Please select an MO user", navigator.getPrimaryStage());
            return;
        }
        OperationResult<Void> result = services.adminService().toggleStatus(Role.MO, mo.getMoId(), !mo.isDisabled());
        DialogUtil.info(result.message(), navigator.getPrimaryStage());
        loadAdminData();
    }

    @FXML
    private void handleAdminToggleJob() {
        Job job = adminJobTable.getSelectionModel().getSelectedItem();
        if (job == null) {
            DialogUtil.error("Please select a job", navigator.getPrimaryStage());
            return;
        }
        if (!job.isOpen()) {
            DialogUtil.info("This job is already closed", navigator.getPrimaryStage());
            return;
        }
        if (!DialogUtil.confirm("Close this job?", navigator.getPrimaryStage())) {
            return;
        }
        OperationResult<Void> result = services.adminService().toggleJobOpenClosed(job.getJobId());
        if (result.success()) {
            DialogUtil.info(result.message(), navigator.getPrimaryStage());
            services.applicationService().rejectPendingApplicationsForJob(job.getJobId());
            loadAdminData();
        } else {
            DialogUtil.error(result.message(), navigator.getPrimaryStage());
        }
    }
}

