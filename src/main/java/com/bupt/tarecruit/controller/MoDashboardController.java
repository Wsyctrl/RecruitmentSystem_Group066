package com.bupt.tarecruit.controller;

import com.bupt.tarecruit.entity.*;
import com.bupt.tarecruit.service.ApplicationService;
import com.bupt.tarecruit.util.DateTimeUtil;
import com.bupt.tarecruit.util.DialogUtil;
import com.bupt.tarecruit.util.OperationResult;
import com.bupt.tarecruit.util.WorkloadRules;
import com.bupt.tarecruit.viewmodel.*;
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
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
/**
 * Controller for the MO dashboard view.
 * Manages job posting, applicant review, profile updates,
 * and administrator operations in the dashboard.
 */
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

    // Admin TA/MO display items
    private final ObservableList<AdminTaDisplay> adminTaItems = FXCollections.observableArrayList();
    private final ObservableList<AdminMoDisplay> adminMoItems = FXCollections.observableArrayList();
    private final ObservableList<AdminJobDisplay> adminJobDisplayItems = FXCollections.observableArrayList();
    private final ObservableList<AdminTaDisplay.JobApplicationInfo> adminTaAppliedJobsItems = FXCollections.observableArrayList();
    private final ObservableList<AdminTaDisplay.JobApplicationInfo> adminTaHiredJobsItems = FXCollections.observableArrayList();
    private final ObservableList<AdminJobDisplay> adminMoJobsItems = FXCollections.observableArrayList();
    private final ObservableList<AccountLogDisplay> accountLogItems = FXCollections.observableArrayList();
    private final ObservableList<JobLogDisplay> jobLogItems = FXCollections.observableArrayList();

    @FXML
    private TabPane tabPane;
    @FXML
    private Tab applicantsTab;
    @FXML
    private Tab myJobsTab;
    @FXML
    private Tab postEditJobTab;
    @FXML
    private Label welcomeLabel;
    @FXML
    private TextField myJobSearchField;
    @FXML
    private TableView<Job> myJobTable;
    @FXML
    private Label selectedJobNameLabel;
    @FXML
    private Label selectedJobStatusLabel;
    @FXML
    private Label selectedJobDatesLabel;
    @FXML
    private Label selectedJobApplicantsLabel;
    @FXML
    private Label selectedJobHiredLabel;
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
    private Tab adminTaTab;
    @FXML
    private Tab adminMoTab;
    @FXML
    private Tab adminJobTab;
    @FXML
    private Tab adminAccountTab;
    @FXML
    private Tab adminJobManagementTab;
    @FXML
    private TableView<Ta> taUserTable;
    @FXML
    private TableView<Mo> moUserTable;
    @FXML
    private TableView<AdminJobDisplay> adminJobTable;

    // New admin tables
    @FXML
    private TableView<AdminTaDisplay> adminTaTable;
    @FXML
    private TableView<AdminMoDisplay> adminMoTable;
    @FXML
    private TableView<AdminTaDisplay.JobApplicationInfo> adminTaAppliedJobsTable;
    @FXML
    private TableView<AdminTaDisplay.JobApplicationInfo> adminTaHiredJobsTable;
    @FXML
    private TableView<AdminJobDisplay> adminMoJobsTable;
    @FXML
    private TableView<AccountLogDisplay> accountLogTable;
    @FXML
    private TableView<JobLogDisplay> jobLogTable;

    // Admin detail labels
    @FXML
    private Label adminTaNameLabel;
    @FXML
    private Label adminTaInfoLabel;
    @FXML
    private Label adminMoNameLabel;
    @FXML
    private Label adminMoInfoLabel;
/**
 * Performs controller-specific initialization after shared dependencies
 * have been injected. Sets up table bindings, search filters, selection
 * listeners, spinner defaults, and admin tab behavior.
 */
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

        // Set row factory to highlight hired rows with yellow background
        applicantTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(ApplicantDisplay item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else if (item.isHired()) {
                    setStyle("-fx-background-color: #fff9c4;"); // Light yellow
                } else {
                    setStyle("");
                }
            }
        });

        positionsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 50, 1));
        setupAdminVisibility(false);

        // Set up admin TA table
        if (adminTaTable != null) {
            adminTaTable.setItems(adminTaItems);
            adminTaTable.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> updateAdminTaDetail(val));
            adminTaTable.setRowFactory(tv -> new TableRow<>() {
                @Override
                protected void updateItem(AdminTaDisplay item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setStyle("");
                    } else if (item.isOverConcurrentThreshold()) {
                        setStyle("-fx-background-color: #ffe6e6;");
                    } else {
                        setStyle("");
                    }
                }
            });
        }

        // Set up admin MO table
        if (adminMoTable != null) {
            adminMoTable.setItems(adminMoItems);
            adminMoTable.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> updateAdminMoDetail(val));
        }

        // Set up admin job display table
        if (adminJobTable != null) {
            adminJobTable.setItems(adminJobDisplayItems);
        }

        // Set up admin TA applied/hired jobs tables
        if (adminTaAppliedJobsTable != null) {
            adminTaAppliedJobsTable.setItems(adminTaAppliedJobsItems);
        }
        if (adminTaHiredJobsTable != null) {
            adminTaHiredJobsTable.setItems(adminTaHiredJobsItems);
            adminTaHiredJobsTable.setRowFactory(tv -> new TableRow<>() {
                @Override
                protected void updateItem(AdminTaDisplay.JobApplicationInfo item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null || item.getJob() == null) {
                        setStyle("");
                        return;
                    }
                    Job job = item.getJob();
                    LocalDate now = LocalDate.now();
                    boolean ongoing = job.getStartDate() != null
                            && job.getEndDate() != null
                            && now.isAfter(job.getStartDate())
                            && now.isBefore(job.getEndDate());
                    if (ongoing) {
                        setStyle("-fx-background-color: #e7f5ff;");
                    } else {
                        setStyle("");
                    }
                }
            });
        }

        // Set up admin MO jobs table
        if (adminMoJobsTable != null) {
            adminMoJobsTable.setItems(adminMoJobsItems);
        }

        // Set up account log table
        if (accountLogTable != null) {
            accountLogTable.setItems(accountLogItems);
        }

        // Set up job log table
        if (jobLogTable != null) {
            jobLogTable.setItems(jobLogItems);
        }

        // When switching to admin tabs, refresh data
        if (tabPane != null) {
            tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
                if (newTab != null) {
                    if (newTab == adminJobTab) {
                        refreshAdminJobs();
                    } else if (newTab == adminTaTab) {
                        refreshAdminTaData();
                    } else if (newTab == adminMoTab) {
                        refreshAdminMoData();
                    } else if (newTab == adminAccountTab) {
                        refreshAccountLogs();
                    } else if (newTab == adminJobManagementTab) {
                        refreshJobLogs();
                    }
                }
            });
        }
    }
/**
 * Reloads all job records for the admin job table.
 * Each job is converted into a display model with hired-count information.
 */
    private void refreshAdminJobs() {
        if (!adminMode) {
            return;
        }
        if (adminJobDisplayItems == null) {
            return;
        }
        List<Job> allJobs = services.jobService().findAllJobs();
        for (Job job : allJobs) {
            services.profileService().findMo(job.getMoId())
                    .map(Mo::getDisplayLabel)
                    .ifPresent(job::setMoName);
        }
        List<AdminJobDisplay> displayItems = allJobs.stream()
                .map(job -> new AdminJobDisplay(job, services.applicationService().countHiredForJob(job.getJobId())))
                .collect(Collectors.toList());
        adminJobDisplayItems.setAll(displayItems);
        if (adminJobTable != null) {
            adminJobTable.refresh();
        }
    }
/**
 * Reloads all TA account data for the admin TA table,
 * including application and hired-job summaries.
 */
    private void refreshAdminTaData() {
        if (!adminMode) {
            return;
        }
        if (adminTaItems == null) {
            return;
        }
        List<Ta> allTa = services.adminService().findAllTa();
        List<Job> allJobs = services.jobService().findAllJobs();

        List<ApplicationRecord> allApplications = services.applicationService().findByJob("*");
        // Actually need to get all applications - let's fix this
        allApplications = services.jobService().findAllJobs().stream()
                .flatMap(job -> services.applicationService().findByJob(job.getJobId()).stream())
                .collect(Collectors.toList());

        Map<String, List<ApplicationRecord>> taApplicationsMap = allApplications.stream()
                .collect(Collectors.groupingBy(ApplicationRecord::getTaId));

        List<AdminTaDisplay> displayItems = allTa.stream()
                .map(ta -> new AdminTaDisplay(ta,
                        taApplicationsMap.getOrDefault(ta.getTaId(), List.of()),
                        allJobs))
                .collect(Collectors.toList());

        adminTaItems.setAll(displayItems);
        if (adminTaTable != null) {
            adminTaTable.refresh();
        }
    }
/**
 * Reloads all MO account data for the admin MO table
 * and groups related jobs for display.
 */
    private void refreshAdminMoData() {
        if (!adminMode) {
            return;
        }
        if (adminMoItems == null) {
            return;
        }
        List<Mo> allMo = services.adminService().findAllMo().stream()
                .filter(mo -> !mo.isAdmin())
                .collect(Collectors.toList());
        List<Job> allJobs = services.jobService().findAllJobs();

        Map<String, List<Job>> moJobsMap = allJobs.stream()
                .collect(Collectors.groupingBy(Job::getMoId));

        List<AdminMoDisplay> displayItems = allMo.stream()
                .map(mo -> new AdminMoDisplay(mo, moJobsMap.getOrDefault(mo.getMoId(), List.of())))
                .collect(Collectors.toList());

        adminMoItems.setAll(displayItems);
        if (adminMoTable != null) {
            adminMoTable.refresh();
        }
    }

    private void updateAdminTaDetail(AdminTaDisplay display) {
        if (display == null) {
            adminTaNameLabel.setText("None selected");
            adminTaInfoLabel.setText("");
            adminTaAppliedJobsItems.clear();
            adminTaHiredJobsItems.clear();
            return;
        }
        Ta ta = display.getTa();
        adminTaNameLabel.setText("Email: " + safeText(ta.getEmail()));
        adminTaInfoLabel.setText(String.format("Name: %s\nPhone: %s\nMajor: %s",
                ta.getDisplayLabel(), safeText(ta.getPhone()), safeText(ta.getMajor())));

        adminTaAppliedJobsItems.setAll(display.getAppliedJobs());
        adminTaHiredJobsItems.setAll(display.getHiredJobs());
    }

    private void updateAdminMoDetail(AdminMoDisplay display) {
        if (display == null) {
            adminMoNameLabel.setText("None selected");
            adminMoInfoLabel.setText("");
            adminMoJobsItems.clear();
            return;
        }
        Mo mo = display.getMo();
        adminMoNameLabel.setText("Email: " + safeText(mo.getEmail()));
        adminMoInfoLabel.setText(String.format("Name: %s | Phone: %s | Modules: %s",
                mo.getDisplayLabel(), safeText(mo.getPhone()), safeText(mo.getResponsibleModules())));

        List<AdminJobDisplay> jobDisplays = display.getJobs().stream()
                .map(job -> new AdminJobDisplay(job, services.applicationService().countHiredForJob(job.getJobId())))
                .collect(Collectors.toList());
        adminMoJobsItems.setAll(jobDisplays);
    }

    private void setupAdminVisibility(boolean enabled) {
        if (tabPane == null) {
            return;
        }
        if (enabled) {
            if (!tabPane.getTabs().contains(adminTaTab)) {
                tabPane.getTabs().add(adminTaTab);
            }
            if (!tabPane.getTabs().contains(adminMoTab)) {
                tabPane.getTabs().add(adminMoTab);
            }
            if (!tabPane.getTabs().contains(adminJobTab)) {
                tabPane.getTabs().add(adminJobTab);
            }
            if (!tabPane.getTabs().contains(adminAccountTab)) {
                tabPane.getTabs().add(adminAccountTab);
            }
            if (!tabPane.getTabs().contains(adminJobManagementTab)) {
                tabPane.getTabs().add(adminJobManagementTab);
            }
        } else {
            tabPane.getTabs().remove(adminTaTab);
            tabPane.getTabs().remove(adminMoTab);
            tabPane.getTabs().remove(adminJobTab);
            tabPane.getTabs().remove(adminAccountTab);
            tabPane.getTabs().remove(adminJobManagementTab);
        }
    }

    @Override
    public void setSession(UserSession session) {
        this.session = session;
        this.adminMode = session.role() == Role.ADMIN;
        welcomeLabel.setText("Welcome, " + session.getDisplayName());
        setupAdminVisibility(adminMode);
        if (adminMode && tabPane != null && adminTaTab != null) {
            tabPane.getSelectionModel().select(adminTaTab);
        } else if (tabPane != null && applicantsTab != null) {
            tabPane.getSelectionModel().select(applicantsTab);
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
        List<Job> jobs = services.jobService().findJobsByMo(currentMoId());
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
            selectedJobStatusLabel.setText("");
            selectedJobDatesLabel.setText("-");
            selectedJobApplicantsLabel.setText("Applicants: 0");
            selectedJobHiredLabel.setText("Current employed: 0");
            selectedJobRequirementsArea.clear();
            selectedJobNotesArea.clear();
            return;
        }
        selectedJobNameLabel.setText(job.getJobName());
        selectedJobStatusLabel.setText("Status: " + job.getStatusLabel());
        selectedJobDatesLabel.setText(DateTimeUtil.formatDate(job.getStartDate()) + " to " + DateTimeUtil.formatDate(job.getEndDate()));
        int count = services.applicationService().findActiveApplicationsForJob(job.getJobId()).size();
        selectedJobApplicantsLabel.setText("Applicants: " + count);
        int hiredCount = services.applicationService().countHiredForJob(job.getJobId());
        selectedJobHiredLabel.setText("Current employed: " + hiredCount);
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
Email: %s
Phone: %s
Major: %s
Skills: %s
Experience: %s
Self-eval: %s
CV: %s
""".formatted(
                ta.getDisplayLabel(),
                safeText(ta.getEmail()),
                safeText(ta.getPhone()),
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
        if (taUserTable != null) {
            taUserTable.setItems(taUsers);
        }
        if (moUserTable != null) {
            moUserTable.setItems(moUsers);
        }
        // Load new admin tables
        refreshAdminTaData();
        refreshAdminMoData();
        refreshAdminJobs();
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
        if (postEditJobTab != null) {
            tabPane.getSelectionModel().select(postEditJobTab);
        }
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
        if (!job.isOpen()) {
            DialogUtil.info("This job is already closed", navigator.getPrimaryStage());
            return;
        }
        if (DialogUtil.confirm("Close this job?", navigator.getPrimaryStage())) {
            OperationResult<Void> result = services.jobService().closeJob(job.getJobId());
            if (result.success()) {
                services.applicationService().rejectPendingApplicationsForJob(job.getJobId());
                // Log the action to JobLog
                String moId = session.moOptional().map(Mo::getMoId).orElse("admin@bupt.edu.cn");
                JobLog log = new JobLog();
                log.setLogId(generateLogId());
                log.setAdminId(moId);
                log.setJobId(job.getJobId());
                log.setAction(JobLog.JobLogAction.CLOSE_JOB);
                log.setPreviousState("Open");
                log.setNewState("Closed");
                log.setTimestamp(java.time.LocalDateTime.now());
                services.jobLogDao().save(log);
                refreshJobLogs();
                refreshMyJobs();
            } else {
                DialogUtil.error(result.message(), navigator.getPrimaryStage());
            }
        }
    }

    @FXML
    private void handleOpenJob() {
        Job job = myJobTable.getSelectionModel().getSelectedItem();
        if (job == null) {
            DialogUtil.error("Please select a job", navigator.getPrimaryStage());
            return;
        }
        if (job.isOpen()) {
            DialogUtil.info("This job is already open", navigator.getPrimaryStage());
            return;
        }
        if (DialogUtil.confirm("Re-open this job?", navigator.getPrimaryStage())) {
            String moId = session.moOptional().map(Mo::getMoId).orElse("admin@bupt.edu.cn");
            OperationResult<Void> result = services.jobService().openJob(job.getJobId());
            if (result.success()) {
                // Log the action to JobLog
                JobLog log = new JobLog();
                log.setLogId(generateLogId());
                log.setAdminId(moId);
                log.setJobId(job.getJobId());
                log.setAction(JobLog.JobLogAction.OPEN_JOB);
                log.setPreviousState("Closed");
                log.setNewState("Open");
                log.setTimestamp(java.time.LocalDateTime.now());
                services.jobLogDao().save(log);
                refreshJobLogs();
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
            if (applicantsTab != null) {
                tabPane.getSelectionModel().select(applicantsTab);
            }
        }
    }

    @FXML
    private void handleHireApplicant() {
        ApplicantDisplay display = applicantTable.getSelectionModel().getSelectedItem();
        if (display == null) {
            DialogUtil.error("Please select an applicant", navigator.getPrimaryStage());
            return;
        }
        ApplicationRecord record = display.getRecord();
        OperationResult<Void> result;

        // If already hired, unhire (back to pending)
        if (record.getStatus() == ApplicationStatus.HIRED) {
            if (DialogUtil.confirm("Unhire this applicant? Status will change back to Pending.", navigator.getPrimaryStage())) {
                result = services.applicationService().unhireApplicant(record.getApplyId());
            } else {
                return;
            }
        } else {
            if (DialogUtil.confirm("Hire this applicant?", navigator.getPrimaryStage())) {
                Optional<Job> currentJobOpt = services.jobService().findById(record.getJobId());
                if (currentJobOpt.isEmpty()) {
                    DialogUtil.error("Job not found", navigator.getPrimaryStage());
                    return;
                }
                List<Job> overlappingJobs = services.applicationService()
                        .findOverlappingHiredJobs(record.getTaId(), record.getJobId());
                if (overlappingJobs.size() >= WorkloadRules.CONCURRENT_JOB_WARNING_THRESHOLD) {
                    String warning = buildConcurrentHireWarning(currentJobOpt.get(), overlappingJobs);
                    if (!DialogUtil.confirmYesNo(warning, navigator.getPrimaryStage())) {
                        return;
                    }
                }
                result = services.applicationService().hireApplicant(record.getApplyId());
            } else {
                return;
            }
        }

        if (result.success()) {
            DialogUtil.info(result.message(), navigator.getPrimaryStage());
            refreshMyJobs();
            loadApplicants(jobSelector.getSelectionModel().getSelectedItem());
        } else {
            DialogUtil.error(result.message(), navigator.getPrimaryStage());
        }
    }

    @FXML
    private void handleRejectApplicant() {
        ApplicantDisplay display = applicantTable.getSelectionModel().getSelectedItem();
        if (display == null) {
            DialogUtil.error("Please select an applicant", navigator.getPrimaryStage());
            return;
        }
        ApplicationRecord record = display.getRecord();
        OperationResult<Void> result;
        // If currently rejected, unreject (back to pending)
        if (record.getStatus() == ApplicationStatus.REJECTED) {
            result = services.applicationService().unrejectApplicant(record.getApplyId());
        } else {
            result = services.applicationService().rejectApplicant(record.getApplyId());
        }
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

    private String buildConcurrentHireWarning(Job targetJob, List<Job> overlappingJobs) {
        String listedJobs = overlappingJobs.stream()
                .sorted(Comparator.comparing(Job::getStartDate, Comparator.nullsLast(LocalDate::compareTo)))
                .map(job -> String.format("- %s (%s to %s)",
                        safeText(job.getJobName()),
                        DateTimeUtil.formatDate(job.getStartDate()),
                        DateTimeUtil.formatDate(job.getEndDate())))
                .collect(Collectors.joining("\n"));
        return String.format(
                "This TA already has %d overlapping hired jobs during the period of \"%s\".\n" +
                        "Do you still want to hire?\n\nExisting overlapping jobs:\n%s",
                overlappingJobs.size(),
                safeText(targetJob.getJobName()),
                listedJobs
        );
    }

    // Admin operations
    @FXML
    private void handleResetTaPassword() {
        AdminTaDisplay display = adminTaTable != null ? adminTaTable.getSelectionModel().getSelectedItem() : null;
        Ta ta = display != null ? display.getTa() : null;
        if (ta == null) {
            // Fallback to old table
            ta = taUserTable.getSelectionModel().getSelectedItem();
        }
        if (ta == null) {
            DialogUtil.error("Please select a TA user", navigator.getPrimaryStage());
            return;
        }
        String adminId = session.moOptional().map(Mo::getMoId).orElse("admin@bupt.edu.cn");
        OperationResult<Void> result = services.adminService().resetPassword(Role.TA, ta.getTaId(), adminId);
        DialogUtil.info(result.message(), navigator.getPrimaryStage());
        loadAdminData();
    }

    @FXML
    private void handleToggleTaStatus() {
        AdminTaDisplay display = adminTaTable != null ? adminTaTable.getSelectionModel().getSelectedItem() : null;
        Ta ta = display != null ? display.getTa() : null;
        if (ta == null) {
            // Fallback to old table
            ta = taUserTable.getSelectionModel().getSelectedItem();
        }
        if (ta == null) {
            DialogUtil.error("Please select a TA user", navigator.getPrimaryStage());
            return;
        }
        String adminId = session.moOptional().map(Mo::getMoId).orElse("admin@bupt.edu.cn");
        OperationResult<Void> result = services.adminService().toggleStatus(Role.TA, ta.getTaId(), !ta.isDisabled(), adminId);
        DialogUtil.info(result.message(), navigator.getPrimaryStage());
        loadAdminData();
    }

    @FXML
    private void handleResetMoPassword() {
        AdminMoDisplay display = adminMoTable != null ? adminMoTable.getSelectionModel().getSelectedItem() : null;
        Mo mo = display != null ? display.getMo() : null;
        if (mo == null) {
            // Fallback to old table
            mo = moUserTable.getSelectionModel().getSelectedItem();
        }
        if (mo == null) {
            DialogUtil.error("Please select an MO user", navigator.getPrimaryStage());
            return;
        }
        String adminId = session.moOptional().map(Mo::getMoId).orElse("admin@bupt.edu.cn");
        OperationResult<Void> result = services.adminService().resetPassword(Role.MO, mo.getMoId(), adminId);
        DialogUtil.info(result.message(), navigator.getPrimaryStage());
        loadAdminData();
    }

    @FXML
    private void handleToggleMoStatus() {
        AdminMoDisplay display = adminMoTable != null ? adminMoTable.getSelectionModel().getSelectedItem() : null;
        Mo mo = display != null ? display.getMo() : null;
        if (mo == null) {
            // Fallback to old table
            mo = moUserTable.getSelectionModel().getSelectedItem();
        }
        if (mo == null) {
            DialogUtil.error("Please select an MO user", navigator.getPrimaryStage());
            return;
        }
        String adminId = session.moOptional().map(Mo::getMoId).orElse("admin@bupt.edu.cn");
        OperationResult<Void> result = services.adminService().toggleStatus(Role.MO, mo.getMoId(), !mo.isDisabled(), adminId);
        DialogUtil.info(result.message(), navigator.getPrimaryStage());
        loadAdminData();
    }

    @FXML
    private void handleAdminToggleJob() {
        AdminJobDisplay display = adminJobTable.getSelectionModel().getSelectedItem();
        if (display == null) {
            DialogUtil.error("Please select a job", navigator.getPrimaryStage());
            return;
        }
        Job job = display.getJob();
        if (!job.isOpen()) {
            DialogUtil.info("This job is already closed", navigator.getPrimaryStage());
            return;
        }
        if (!DialogUtil.confirm("Close this job?", navigator.getPrimaryStage())) {
            return;
        }
        String adminId = session.moOptional().map(Mo::getMoId).orElse("admin@bupt.edu.cn");
        OperationResult<Void> result = services.adminService().toggleJobOpenClosed(job.getJobId());
        if (result.success()) {
            DialogUtil.info(result.message(), navigator.getPrimaryStage());
            services.applicationService().rejectPendingApplicationsForJob(job.getJobId());
            // Log the action to JobLog
            JobLog log = new JobLog();
            log.setLogId(generateLogId());
            log.setAdminId(adminId);
            log.setJobId(job.getJobId());
            log.setAction(JobLog.JobLogAction.CLOSE_JOB);
            log.setPreviousState("Open");
            log.setNewState("Closed");
            log.setTimestamp(java.time.LocalDateTime.now());
            services.jobLogDao().save(log);
            refreshJobLogs();
            refreshAdminJobs();
        } else {
            DialogUtil.error(result.message(), navigator.getPrimaryStage());
        }
    }

    @FXML
    private void handleAdminOpenJob() {
        AdminJobDisplay display = adminJobTable.getSelectionModel().getSelectedItem();
        if (display == null) {
            DialogUtil.error("Please select a job", navigator.getPrimaryStage());
            return;
        }
        Job job = display.getJob();
        if (job.isOpen()) {
            DialogUtil.info("This job is already open", navigator.getPrimaryStage());
            return;
        }
        if (!DialogUtil.confirm("Re-open this job?", navigator.getPrimaryStage())) {
            return;
        }
        String adminId = session.moOptional().map(Mo::getMoId).orElse("admin@bupt.edu.cn");
        OperationResult<Void> result = services.jobService().openJob(job.getJobId());
        if (result.success()) {
            DialogUtil.info(result.message(), navigator.getPrimaryStage());
            // Log the action to JobLog
            JobLog log = new JobLog();
            log.setLogId(generateLogId());
            log.setAdminId(adminId);
            log.setJobId(job.getJobId());
            log.setAction(JobLog.JobLogAction.OPEN_JOB);
            log.setPreviousState("Closed");
            log.setNewState("Open");
            log.setTimestamp(java.time.LocalDateTime.now());
            services.jobLogDao().save(log);
            refreshJobLogs();
            refreshAdminJobs();
        } else {
            DialogUtil.error(result.message(), navigator.getPrimaryStage());
        }
    }

    private void refreshAccountLogs() {
        if (!adminMode || accountLogItems == null) {
            return;
        }
        List<AccountLogDisplay> logs = services.adminService().findAllAccountLogs().stream()
                .map(AccountLogDisplay::new)
                .collect(Collectors.toList());
        accountLogItems.setAll(logs);
        if (accountLogTable != null) {
            accountLogTable.refresh();
        }
    }

    private void updateJobManagementDetail(AdminJobDisplay display) {
        // No longer used
    }

    private void refreshJobLogs() {
        if (!adminMode || jobLogItems == null) {
            return;
        }
        List<JobLog> allLogs = services.jobLogDao().findAll();
        List<Job> allJobs = services.jobService().findAllJobs();

        // Create a map of jobId to Job
        Map<String, Job> jobMap = allJobs.stream()
                .collect(Collectors.toMap(Job::getJobId, Function.identity(), (a, b) -> a));

        // Create display items
        List<JobLogDisplay> jobLogs = allLogs.stream()
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .map(log -> new JobLogDisplay(log, jobMap.get(log.getJobId())))
                .collect(Collectors.toList());

        jobLogItems.setAll(jobLogs);
        if (jobLogTable != null) {
            jobLogTable.refresh();
        }
    }

    @FXML
    private void handleToggleJobManagementDisabled() {
        // Removed - job management is done in All Jobs tab
    }

    @FXML
    private void handleRefreshJobManagement() {
        // Removed - job management is done in All Jobs tab
    }

    @FXML
    private void handleRefreshJobLogs() {
        refreshJobLogs();
    }

    private String generateLogId() {
        List<String> existing = services.accountLogDao().findAll().stream()
                .map(com.bupt.tarecruit.entity.AccountLog::getLogId)
                .collect(Collectors.toList());
        return com.bupt.tarecruit.util.IdGenerator.nextId("log", existing);
    }

    @FXML
    private void handleRefreshAccountLogs() {
        refreshAccountLogs();
    }
}
