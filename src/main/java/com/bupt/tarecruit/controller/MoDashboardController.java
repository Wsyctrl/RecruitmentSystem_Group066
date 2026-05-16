package com.bupt.tarecruit.controller;

import com.bupt.tarecruit.entity.*;
import com.bupt.tarecruit.service.AiService;
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
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.concurrent.Task;

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
    private long applicantsAiContextVersion = 0L;
    private Task<List<AiService.ApplicantRecommendation>> activeRecommendApplicantsTask;
    private Task<List<AiService.ApplicantRecommendation>> activeSimilarApplicantsTask;
    private Task<List<String>> activeKeywordTask;

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

    // Applicant card state
    private ApplicantDisplay selectedApplicant;
    private final java.util.Map<String, String> applicantSummaries = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Set<String> top3ApplicantIds = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());
    private boolean isTop3FilterActive = false;

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
    private TextArea keywordsField;
    @FXML
    private Button generateKeywordsBtn;
    @FXML
    private Label keywordsLoadingLabel;
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
    private FlowPane applicantCardPane;
    @FXML
    private ScrollPane applicantCardScroll;
    @FXML
    private Label applicantNameLabel;
    @FXML
    private Label applicantStatusLabel;
    @FXML
    private TextArea applicantProfileArea;
    @FXML
    private TextArea aiApplicantResultArea;
    @FXML
    private TextArea aiKeywordsArea;

    @FXML
    private TextField moFullNameField;
    @FXML
    private TextField moPhoneField;
    @FXML
    private TextField moEmailField;
    @FXML
    private TextArea moModuleArea;

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
        jobSelector.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
            invalidateApplicantsAiContext();
            loadApplicants(val);
        });

        filteredApplicants = new FilteredList<>(applicantItems, item -> true);
        applicantSearchField.textProperty().addListener((obs, old, val) -> filterApplicants(val));

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
                if (oldTab == applicantsTab && newTab != applicantsTab) {
                    invalidateApplicantsAiContext();
                }
                if (newTab == applicantsTab) {
                    invalidateApplicantsAiContext();
                }
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
        invalidateApplicantsAiContext();
        isTop3FilterActive = false;
        top3ApplicantIds.clear();
        applicantSummaries.clear();
        selectedApplicant = null;

        if (job == null) {
            applicantItems.clear();
            renderApplicantCards();
            return;
        }
        List<Ta> allTa = services.adminService().findAllTa();
        Map<String, Ta> taMap = allTa.stream().collect(Collectors.toMap(Ta::getTaId, Function.identity(), (a, b) -> a));
        ApplicationService applicationService = services.applicationService();
        applicantItems.setAll(applicationService.findActiveApplicationsForJob(job.getJobId()).stream()
                .map(record -> new ApplicantDisplay(record, taMap.get(record.getTaId())))
                .collect(Collectors.toList()));

        // Pre-load summaries from cache for instant display
        for (ApplicantDisplay applicant : applicantItems) {
            if (applicant.getTa() != null) {
                String cached = services.fileStorageHelper().loadSummary(applicant.getTaId());
                if (!cached.isEmpty()) {
                    applicantSummaries.put(applicant.getTaId(), cached);
                }
            }
        }

        filteredApplicants.setPredicate(item -> true);
        renderApplicantCards();
        generateAllSummariesInBackground();
    }

    private void filterApplicants(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            filteredApplicants.setPredicate(item -> true);
        } else {
            String lower = keyword.toLowerCase().trim();
            filteredApplicants.setPredicate(applicant -> {
                // Search in TA ID (case-insensitive)
                if (applicant.getTaId().toLowerCase().contains(lower)) {
                    return true;
                }
                // Search in name (case-insensitive)
                if (applicant.getTaName() != null && applicant.getTaName().toLowerCase().contains(lower)) {
                    return true;
                }
                // Search in phone (case-insensitive)
                if (applicant.getPhone() != null && applicant.getPhone().toLowerCase().contains(lower)) {
                    return true;
                }
                // Search in email (case-insensitive)
                if (applicant.getEmail() != null && applicant.getEmail().toLowerCase().contains(lower)) {
                    return true;
                }
                // Search in status (case-insensitive)
                if (applicant.getStatus() != null && applicant.getStatus().toLowerCase().contains(lower)) {
                    return true;
                }
                // Search in major (case-insensitive)
                if (applicant.getTa() != null && applicant.getTa().getMajor() != null
                        && applicant.getTa().getMajor().toLowerCase().contains(lower)) {
                    return true;
                }
                return false;
            });
        }
        renderApplicantCards();
    }

    private List<ApplicantDisplay> currentApplicants() {
        Job selectedJob = jobSelector.getSelectionModel().getSelectedItem();
        if (selectedJob == null) {
            return List.of();
        }
        return services.applicationService().findActiveApplicationsForJob(selectedJob.getJobId()).stream()
                .map(record -> new ApplicantDisplay(record, services.profileService().findTa(record.getTaId()).orElse(null)))
                .collect(Collectors.toList());
    }

    private long captureApplicantsAiContext() {
        return applicantsAiContextVersion;
    }

    private boolean isApplicantsAiContextValid(long contextToken, String jobIdSnapshot) {
        if (contextToken != applicantsAiContextVersion) {
            return false;
        }
        if (tabPane == null || tabPane.getSelectionModel().getSelectedItem() != applicantsTab) {
            return false;
        }
        Job currentJob = jobSelector.getSelectionModel().getSelectedItem();
        return currentJob != null && currentJob.getJobId().equalsIgnoreCase(jobIdSnapshot);
    }

    private void invalidateApplicantsAiContext() {
        applicantsAiContextVersion++;
        if (activeRecommendApplicantsTask != null && activeRecommendApplicantsTask.isRunning()) {
            activeRecommendApplicantsTask.cancel(true);
        }
        if (activeSimilarApplicantsTask != null && activeSimilarApplicantsTask.isRunning()) {
            activeSimilarApplicantsTask.cancel(true);
        }
        if (activeKeywordTask != null && activeKeywordTask.isRunning()) {
            activeKeywordTask.cancel(true);
        }
        clearApplicantsAiOutputs();
    }

    private void clearApplicantsAiOutputs() {
        if (aiApplicantResultArea != null) {
            aiApplicantResultArea.clear();
        }
        if (aiKeywordsArea != null) {
            aiKeywordsArea.clear();
        }
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
        keywordsField.clear();
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
        keywordsField.setText(safeText(job.getKeywords()));
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
            job.setKeywords(keywordsField.getText());
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
    private void handleRefreshApplicants() {
        invalidateApplicantsAiContext();
        loadApplicants(jobSelector.getSelectionModel().getSelectedItem());
    }

    @FXML
    private void handleClearApplicantSearch() {
        applicantSearchField.clear();
        filterApplicants(null);
    }

    @FXML
    private void handleAiGenerateInsights() {
        showInsightsDialog();
    }

    private void showInsightsDialog() {
        try {
            java.net.URL fxmlUrl = getClass().getResource("/fxml/insights-dialog.fxml");
            if (fxmlUrl == null) {
                DialogUtil.error("Cannot find insights-dialog.fxml resource", navigator.getPrimaryStage());
                return;
            }

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(fxmlUrl);
            javafx.scene.Parent root = loader.load();

            InsightsDialogController controller = loader.getController();
            if (controller == null) {
                DialogUtil.error("Failed to load insights controller", navigator.getPrimaryStage());
                return;
            }
            controller.setServices(services);

            // Create dialog stage
            Stage dialogStage = new Stage();
            dialogStage.setTitle("30-Day Hiring Insights");
            dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialogStage.initOwner(navigator.getPrimaryStage());
            dialogStage.setResizable(false);

            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.getStylesheets().add(
                    getClass().getResource("/css/application.css").toExternalForm());
            dialogStage.setScene(scene);

            controller.setDialogStage(dialogStage);
            controller.loadData();

            dialogStage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            DialogUtil.error("Failed to open insights: " + e.getClass().getSimpleName() + " - " + e.getMessage(), navigator.getPrimaryStage());
        }
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
    private void handleChangePasswordFromProfile() {
        Mo mo = session.moOptional().orElse(null);
        if (mo == null) {
            return;
        }

        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/fxml/change-password-dialog.fxml"));
            javafx.scene.Parent root = loader.load();

            ChangePasswordDialogController controller = loader.getController();
            controller.setServices(services);
            controller.setMoId(mo.getMoId());

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
    private void handleLogout() {
        navigator.showLogin();
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private String formatApplicantRecommendations(List<AiService.ApplicantRecommendation> recommendations) {
        if (recommendations == null || recommendations.isEmpty()) {
            return "No AI applicant results were returned.";
        }
        return recommendations.stream()
                .map(item -> "• " + item.taId() + " | Match Score: " + item.score() + "\n  " + item.reason())
                .collect(Collectors.joining("\n\n"));
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

    // ========================================
    // Applicant Card Methods
    // ========================================

    private void renderApplicantCards() {
        if (applicantCardPane == null) {
            return;
        }
        applicantCardPane.getChildren().clear();

        // Get the filtered applicants and optionally sort by top 3
        List<ApplicantDisplay> toRender = new java.util.ArrayList<>(filteredApplicants);

        if (isTop3FilterActive && !top3ApplicantIds.isEmpty()) {
            toRender.sort((a, b) -> {
                boolean aTop = top3ApplicantIds.contains(a.getTaId());
                boolean bTop = top3ApplicantIds.contains(b.getTaId());
                if (aTop && !bTop) return -1;
                if (!aTop && bTop) return 1;
                return 0;
            });
        }

        for (ApplicantDisplay applicant : toRender) {
            applicantCardPane.getChildren().add(createApplicantCard(applicant));
        }
    }

    private javafx.scene.layout.VBox createApplicantCard(ApplicantDisplay applicant) {
        javafx.scene.layout.VBox card = new javafx.scene.layout.VBox();
        card.setSpacing(8);
        card.getStyleClass().addAll("applicant-card");

        if (applicant.isHired()) {
            card.getStyleClass().add("applicant-card-hired");
        }
        if (top3ApplicantIds.contains(applicant.getTaId())) {
            card.getStyleClass().add("applicant-card-recommended");
        }
        if (selectedApplicant != null && selectedApplicant.getTaId().equals(applicant.getTaId())) {
            card.getStyleClass().add("applicant-card-selected");
        }

        // Header with name and status
        javafx.scene.layout.HBox header = new javafx.scene.layout.HBox();
        header.setSpacing(8);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label nameLabel = new Label(applicant.getTaName());
        nameLabel.getStyleClass().add("applicant-card-name");

        Label statusLabel = new Label(applicant.getStatus());
        statusLabel.getStyleClass().addAll("applicant-card-status");
        if (applicant.isHired()) {
            statusLabel.getStyleClass().add("hired");
        } else if (applicant.getStatus().equalsIgnoreCase("Rejected")) {
            statusLabel.getStyleClass().add("rejected");
        } else {
            statusLabel.getStyleClass().add("pending");
        }

        header.getChildren().addAll(nameLabel, statusLabel);

        // AI Summary
        String summary = applicantSummaries.get(applicant.getTaId());
        Label summaryLabel = new Label(summary != null ? summary : "Analyzing profile...");
        summaryLabel.getStyleClass().add("applicant-card-summary");
        summaryLabel.setWrapText(true);

        // Recommend badge
        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox();
        content.setSpacing(6);
        content.getChildren().add(header);

        if (top3ApplicantIds.contains(applicant.getTaId())) {
            Label badge = new Label("★ TOP MATCH");
            badge.getStyleClass().add("applicant-card-recommend-badge");
            content.getChildren().add(badge);
        }

        content.getChildren().add(summaryLabel);

        card.getChildren().add(content);

        // Click handler
        card.setOnMouseClicked(e -> {
            selectedApplicant = applicant;
            updateApplicantDetail(applicant);
            renderApplicantCards();
        });

        return card;
    }

    private void generateAllSummariesInBackground() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                for (ApplicantDisplay applicant : applicantItems) {
                    if (isCancelled()) break;
                    if (applicant.getTa() == null) continue;
                    String taId = applicant.getTaId();

                    // Skip if already loaded
                    if (applicantSummaries.containsKey(taId)) continue;

                    // Try loading from cache first
                    String cached = services.fileStorageHelper().loadSummary(taId);
                    if (!cached.isEmpty()) {
                        applicantSummaries.put(taId, cached);
                        javafx.application.Platform.runLater(() -> renderApplicantCards());
                        continue;
                    }

                    // Generate new summary
                    try {
                        String summary = services.aiService().generateApplicantSummary(applicant.getTa());
                        applicantSummaries.put(taId, summary);
                        // Save to cache
                        services.fileStorageHelper().saveSummary(taId, summary);
                        javafx.application.Platform.runLater(() -> renderApplicantCards());
                    } catch (Exception ex) {
                        // Fallback summary
                        String fallback = applicant.getTa().getMajor() + " major with skills in " +
                                (applicant.getTa().getSkills() != null && !applicant.getTa().getSkills().isBlank()
                                        ? applicant.getTa().getSkills().split(",")[0] : "various areas");
                        applicantSummaries.put(taId, fallback);
                        javafx.application.Platform.runLater(() -> renderApplicantCards());
                    }
                }
                return null;
            }
        };
        new Thread(task, "applicant-summary-generator").start();
    }

    @FXML
    private void handleAiTop3Filter() {
        Job job = jobSelector.getSelectionModel().getSelectedItem();
        if (job == null) {
            DialogUtil.error("Please select a job first", navigator.getPrimaryStage());
            return;
        }
        List<ApplicantDisplay> applicants = currentApplicants();
        if (applicants.isEmpty()) {
            DialogUtil.error("No applicants for this job", navigator.getPrimaryStage());
            return;
        }

        aiKeywordsArea.setText("AI is selecting top 3 candidates...");
        Task<List<AiService.ApplicantRecommendation>> task = new Task<>() {
            @Override
            protected List<AiService.ApplicantRecommendation> call() throws Exception {
                return services.aiService().recommendApplicantsForJob(job, applicants);
            }
        };

        task.setOnSucceeded(evt -> {
            List<AiService.ApplicantRecommendation> recommendations = task.getValue();
            top3ApplicantIds.clear();
            if (recommendations.size() >= 3) {
                for (int i = 0; i < 3; i++) {
                    top3ApplicantIds.add(recommendations.get(i).taId());
                }
            } else {
                for (AiService.ApplicantRecommendation rec : recommendations) {
                    top3ApplicantIds.add(rec.taId());
                }
            }
            isTop3FilterActive = true;

            // Apply current search filter with reordering
            filterApplicants(applicantSearchField.getText());

            StringBuilder sb = new StringBuilder("Top Candidates:\n\n");
            for (int i = 0; i < Math.min(3, recommendations.size()); i++) {
                AiService.ApplicantRecommendation rec = recommendations.get(i);
                sb.append(String.format("%d. %s (Score: %d)\n   %s\n\n",
                        i + 1, rec.taId(), rec.score(), rec.reason()));
            }
            aiKeywordsArea.setText(sb.toString());
        });

        task.setOnFailed(evt -> {
            aiKeywordsArea.setText("AI selection failed: " + task.getException().getMessage());
        });

        new Thread(task, "ai-top3-filter").start();
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

    @FXML
    private void handleHireApplicant() {
        if (selectedApplicant == null) {
            DialogUtil.error("Please select an applicant first", navigator.getPrimaryStage());
            return;
        }
        ApplicantDisplay display = selectedApplicant;
        ApplicationRecord record = display.getRecord();
        OperationResult<Void> result;

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
        if (selectedApplicant == null) {
            DialogUtil.error("Please select an applicant first", navigator.getPrimaryStage());
            return;
        }
        ApplicantDisplay display = selectedApplicant;
        ApplicationRecord record = display.getRecord();
        OperationResult<Void> result;
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
        if (selectedApplicant == null || selectedApplicant.getTa() == null) {
            DialogUtil.error("Please select an applicant first", navigator.getPrimaryStage());
            return;
        }
        ApplicantDisplay display = selectedApplicant;
        Ta ta = display.getTa();
        Path source = services.fileStorageHelper().resolveCvFile(ta.getTaId(), ta.getCvPath());
        if (!Files.isRegularFile(source)) {
            DialogUtil.error("CV file not found", navigator.getPrimaryStage());
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
    private void handleAiGenerateJobKeywords() {
        Job job = jobSelector.getSelectionModel().getSelectedItem();
        if (job == null) {
            DialogUtil.error("Please select a job first", navigator.getPrimaryStage());
            return;
        }
        long contextToken = captureApplicantsAiContext();
        String jobIdSnapshot = job.getJobId();
        aiKeywordsArea.setText("AI is generating keywords...");
        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() throws Exception {
                if (isCancelled()) {
                    return List.of();
                }
                return services.aiService().generateJobKeywords(job);
            }
        };
        activeKeywordTask = task;
        task.setOnSucceeded(evt -> {
            if (!isApplicantsAiContextValid(contextToken, jobIdSnapshot)) {
                return;
            }
            aiKeywordsArea.setText(task.getValue().stream()
                    .map(k -> "• " + k)
                    .collect(Collectors.joining("\n")));
        });
        task.setOnCancelled(evt -> {
            if (isApplicantsAiContextValid(contextToken, jobIdSnapshot)) {
                aiKeywordsArea.clear();
            }
        });
        task.setOnFailed(evt -> aiKeywordsArea.setText("Keyword generation failed: " + task.getException().getMessage()));
        new Thread(task, "ai-job-keywords").start();
    }

    @FXML
    private void handleAiRecommendApplicants() {
        Job job = jobSelector.getSelectionModel().getSelectedItem();
        if (job == null) {
            DialogUtil.error("Please select a job first", navigator.getPrimaryStage());
            return;
        }
        List<ApplicantDisplay> applicants = currentApplicants();
        if (applicants.isEmpty()) {
            aiApplicantResultArea.setText("There are no applicants for the current job.");
            return;
        }
        long contextToken = captureApplicantsAiContext();
        String jobIdSnapshot = job.getJobId();
        aiApplicantResultArea.setText("AI is ranking applicants...");
        Task<List<AiService.ApplicantRecommendation>> task = new Task<>() {
            @Override
            protected List<AiService.ApplicantRecommendation> call() throws Exception {
                if (isCancelled()) {
                    return List.of();
                }
                return services.aiService().recommendApplicantsForJob(job, applicants);
            }
        };
        activeRecommendApplicantsTask = task;
        task.setOnSucceeded(evt -> {
            if (!isApplicantsAiContextValid(contextToken, jobIdSnapshot)) {
                return;
            }
            aiApplicantResultArea.setText(formatApplicantRecommendations(task.getValue()));
        });
        task.setOnCancelled(evt -> {
            if (isApplicantsAiContextValid(contextToken, jobIdSnapshot)) {
                aiApplicantResultArea.clear();
            }
        });
        task.setOnFailed(evt -> aiApplicantResultArea.setText("AI ranking failed: " + task.getException().getMessage()));
        new Thread(task, "ai-recommend-applicants").start();
    }

    @FXML
    private void handleGenerateJobKeywords() {
        String requirements = requirementsField.getText();
        String notes = notesField.getText();
        String moduleName = moduleField.getText();
        String jobName = jobNameField.getText();

        if (requirements == null || requirements.isBlank()) {
            DialogUtil.error("Please enter job requirements first", navigator.getPrimaryStage());
            return;
        }

        generateKeywordsBtn.setDisable(true);
        keywordsLoadingLabel.setVisible(true);

        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() throws Exception {
                // Create a temporary Job object for AI analysis
                Job tempJob = new Job();
                tempJob.setJobName(jobName);
                tempJob.setModuleName(moduleName);
                tempJob.setRequirements(requirements);
                tempJob.setAdditionalNotes(notes);
                return services.aiService().generateJobKeywords(tempJob);
            }
        };

        task.setOnSucceeded(evt -> {
            List<String> keywords = task.getValue();
            keywordsField.setText(keywords.stream()
                    .map(k -> "• " + k)
                    .collect(Collectors.joining("\n")));
            generateKeywordsBtn.setDisable(false);
            keywordsLoadingLabel.setVisible(false);
        });

        task.setOnFailed(evt -> {
            DialogUtil.error("Failed to generate keywords: " + task.getException().getMessage(), navigator.getPrimaryStage());
            generateKeywordsBtn.setDisable(false);
            keywordsLoadingLabel.setVisible(false);
        });

        new Thread(task, "job-keywords-generator").start();
    }
}
