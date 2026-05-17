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
    /** TA ids that the most recent AI recommendation flagged as top matches (size = MO-chosen N). */
    private final java.util.Set<String> topMatchApplicantIds = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());
    /** AI score for each ranked TA (taId → score). Empty when AI hasn't run for this job yet. */
    private final java.util.Map<String, Integer> top3Scores = new java.util.concurrent.ConcurrentHashMap<>();
    private boolean isTopMatchActive = false;

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
    private TextArea selectedJobKeywordsArea;

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
    private TextArea aiPreferenceArea;
    @FXML
    private Spinner<Integer> aiTopCountSpinner;
    @FXML
    private Button hireButton;
    @FXML
    private Button rejectButton;

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
    private Tab adminInsightsTab;
    @FXML
    private Tab adminAccountTab;
    @FXML
    private Tab adminJobManagementTab;
    @FXML
    private javafx.scene.layout.VBox applicantDetailPanel;
    @FXML
    private javafx.scene.layout.VBox myJobsDetailPanel;
    @FXML
    private SplitPane applicantsSplitPane;
    @FXML
    private SplitPane applicantDetailSplit;
    @FXML
    private SplitPane myJobsSplitPane;
    @FXML
    private SplitPane adminTaSplitPane;
    @FXML
    private SplitPane adminMoSplitPane;
    @FXML
    private javafx.scene.layout.StackPane applicantsLeftPane;
    @FXML
    private Label applicantsEmptyLabel;
    @FXML
    private InsightsDialogController insightsViewController;
    private boolean insightsLoaded;
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
        // Search only triggers on Refresh button; do not auto-filter on each keystroke.

        // Lock split-pane dividers at center.
        lockSplitDivider(applicantsSplitPane, 0.5);
        lockSplitDivider(myJobsSplitPane, 0.5);
        lockSplitDivider(adminTaSplitPane, 0.5);
        lockSplitDivider(adminMoSplitPane, 0.5);
        // The right-pane vertical split (profile vs. AI panel) stays user-resizable.
        // Its initial position (0.5) is set in FXML; do not lock it here.

        // AI Top-N count: 1..10, default 3, never null.
        if (aiTopCountSpinner != null) {
            aiTopCountSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 3));
            aiTopCountSpinner.setEditable(true);
        }

        // Configure FlowPane: cards size themselves relative to the container width.
        if (applicantCardPane != null) {
            // A huge wrap length effectively disables FlowPane's own wrap logic; each row holds
            // exactly two children because the bound card width is paneWidth/2.
            applicantCardPane.setPrefWrapLength(Double.MAX_VALUE);
        }
        if (applicantCardScroll != null) {
            applicantCardScroll.setFitToWidth(true);
            applicantCardScroll.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        }

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
                        autoSelectFirst(adminJobTable);
                    } else if (newTab == adminTaTab) {
                        refreshAdminTaData();
                        autoSelectFirst(adminTaTable);
                    } else if (newTab == adminMoTab) {
                        refreshAdminMoData();
                        autoSelectFirst(adminMoTable);
                    } else if (newTab == adminAccountTab) {
                        refreshAccountLogs();
                    } else if (newTab == adminJobManagementTab) {
                        refreshJobLogs();
                    } else if (newTab == adminInsightsTab) {
                        ensureInsightsLoaded();
                    } else if (newTab == applicantsTab) {
                        autoSelectFirstApplicantCard();
                    } else if (newTab == myJobsTab) {
                        autoSelectFirst(myJobTable);
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
            if (!tabPane.getTabs().contains(adminInsightsTab)) {
                tabPane.getTabs().add(adminInsightsTab);
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
            tabPane.getTabs().remove(adminInsightsTab);
            tabPane.getTabs().remove(adminAccountTab);
            tabPane.getTabs().remove(adminJobManagementTab);
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
        // Defer node lookup until after the scene is laid out.
        javafx.application.Platform.runLater(() ->
                split.lookupAll(".split-pane-divider").forEach(node -> node.setMouseTransparent(true)));
    }

    private <T> void autoSelectFirst(TableView<T> table) {
        if (table == null || table.getItems() == null || table.getItems().isEmpty()) return;
        if (table.getSelectionModel().getSelectedItem() == null) {
            table.getSelectionModel().selectFirst();
        }
    }

    private void autoSelectFirstApplicantCard() {
        if (selectedApplicant == null && !filteredApplicants.isEmpty()) {
            selectedApplicant = filteredApplicants.get(0);
            updateApplicantDetail(selectedApplicant);
            renderApplicantCards();
        }
    }

    private void ensureInsightsLoaded() {
        if (insightsViewController == null) return;
        if (!insightsLoaded) {
            insightsViewController.setServices(services);
            insightsViewController.loadData();
            insightsLoaded = true;
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
        // Preserve current selections across refresh so hiring/closing actions don't
        // bounce the MO back to the first job.
        Job priorJobsSelection = myJobTable.getSelectionModel().getSelectedItem();
        Job priorSelectorSelection = jobSelector.getSelectionModel().getSelectedItem();

        List<Job> jobs = services.jobService().findJobsByMo(currentMoId());
        jobs.sort(Comparator.comparing(Job::getJobId));
        myJobs.setAll(jobs);
        // Applicants page only acts on open jobs; closed jobs cannot accept new hires.
        List<Job> openJobs = jobs.stream().filter(Job::isOpen).collect(Collectors.toList());
        jobOptions.setAll(openJobs);
        if (jobs.isEmpty()) {
            updateSelectedJob(null);
            return;
        }
        Job restoredTableJob = priorJobsSelection == null
                ? null
                : jobs.stream().filter(j -> j.getJobId().equalsIgnoreCase(priorJobsSelection.getJobId())).findFirst().orElse(null);
        if (restoredTableJob != null) {
            myJobTable.getSelectionModel().select(restoredTableJob);
        } else {
            myJobTable.getSelectionModel().selectFirst();
        }
        Job restoredSelectorJob = priorSelectorSelection == null
                ? null
                : openJobs.stream().filter(j -> j.getJobId().equalsIgnoreCase(priorSelectorSelection.getJobId())).findFirst().orElse(null);
        if (restoredSelectorJob != null) {
            jobSelector.getSelectionModel().select(restoredSelectorJob);
        } else if (priorSelectorSelection == null && !openJobs.isEmpty()) {
            jobSelector.getSelectionModel().selectFirst();
        } else if (priorSelectorSelection != null && restoredSelectorJob == null) {
            // Previously selected job is no longer open: clear so the Applicants view doesn't
            // keep showing data from a closed posting.
            jobSelector.getSelectionModel().clearSelection();
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
            if (selectedJobKeywordsArea != null) {
                selectedJobKeywordsArea.clear();
            }
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
        if (selectedJobKeywordsArea != null) {
            selectedJobKeywordsArea.setText(safeText(job.getKeywords()));
        }
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
        isTopMatchActive = false;
        topMatchApplicantIds.clear();
        top3Scores.clear();
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
                // A disabled TA cannot remain a hire candidate: hide their still-pending entries.
                // Their historical Hired/Rejected entries stay visible for audit purposes.
                .filter(display -> !(display.getRecord().getStatus() == ApplicationStatus.PENDING
                        && display.getTa() != null
                        && display.getTa().isDisabled()))
                .collect(Collectors.toList()));

        // Pre-load summaries from the TA record so the cards render instantly when cached.
        for (ApplicantDisplay applicant : applicantItems) {
            Ta ta = applicant.getTa();
            if (ta != null && ta.getAiSummary() != null && !ta.getAiSummary().isBlank()) {
                applicantSummaries.put(applicant.getTaId(), ta.getAiSummary().trim());
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
                // Disabled TAs can't be hired, so they must not be considered for AI ranking either.
                // Their historical Hired/Rejected rows are still visible elsewhere, but pending
                // entries from disabled TAs are dropped here to mirror loadApplicants().
                .filter(display -> !(display.getRecord().getStatus() == ApplicationStatus.PENDING
                        && display.getTa() != null
                        && display.getTa().isDisabled()))
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
        filterJobs(myJobSearchField.getText());
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
        if (job == null) {
            return;
        }
        if (!job.isOpen()) {
            DialogUtil.info("This job is closed. Re-open it before reviewing applicants.",
                    navigator.getPrimaryStage());
            return;
        }
        jobSelector.getSelectionModel().select(job);
        loadApplicants(job);
        if (applicantsTab != null) {
            tabPane.getSelectionModel().select(applicantsTab);
        }
    }

    @FXML
    private void handleRefreshApplicants() {
        // Apply current search filter; this is the only way to trigger search.
        filterApplicants(applicantSearchField.getText());
    }

    @FXML
    private void handleClearApplicantSearch() {
        // Retained for backwards compatibility; no longer wired into the FXML.
        applicantSearchField.clear();
        filterApplicants(null);
    }

    @FXML
    private void handleAiGenerateInsights() {
        // Insights now live as their own admin tab; selecting that tab triggers loading.
        if (tabPane != null && adminInsightsTab != null) {
            tabPane.getSelectionModel().select(adminInsightsTab);
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

    private static String normalizeTaId(String taId) {
        return taId == null ? "" : taId.trim().toLowerCase(java.util.Locale.ROOT);
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

    @FXML
    private void handleClearAccountLogs() {
        if (!DialogUtil.confirm("Clear all account-management log entries? This cannot be undone.", navigator.getPrimaryStage())) {
            return;
        }
        try {
            java.nio.file.Path file = services.getDataDir().resolve("AccountLogs.csv");
            com.bupt.tarecruit.util.CsvUtil.writeAll(file,
                    new String[]{"log_id", "admin_id", "target_user_id", "target_role", "action", "previous_state", "new_state", "timestamp"},
                    new java.util.ArrayList<>());
            refreshAccountLogs();
            DialogUtil.info("Account-management log cleared.", navigator.getPrimaryStage());
        } catch (Exception ex) {
            DialogUtil.error("Failed to clear log: " + ex.getMessage(), navigator.getPrimaryStage());
        }
    }

    @FXML
    private void handleClearJobLogs() {
        if (!DialogUtil.confirm("Clear all job-management log entries? This cannot be undone.", navigator.getPrimaryStage())) {
            return;
        }
        try {
            java.nio.file.Path file = services.getDataDir().resolve("JobLogs.csv");
            com.bupt.tarecruit.util.CsvUtil.writeAll(file,
                    new String[]{"log_id", "admin_id", "job_id", "action", "previous_state", "new_state", "timestamp"},
                    new java.util.ArrayList<>());
            refreshJobLogs();
            DialogUtil.info("Job-management log cleared.", navigator.getPrimaryStage());
        } catch (Exception ex) {
            DialogUtil.error("Failed to clear log: " + ex.getMessage(), navigator.getPrimaryStage());
        }
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

        // Toggle the empty-state label according to the filtered view; favor the explicit
        // "no applicants" message when the underlying job has zero applicants at all.
        if (applicantsEmptyLabel != null) {
            boolean isEmpty = filteredApplicants == null || filteredApplicants.isEmpty();
            boolean noUnderlyingApplicants = applicantItems.isEmpty();
            if (isEmpty) {
                applicantsEmptyLabel.setText(noUnderlyingApplicants
                        ? "No applicants for this job yet."
                        : "No applicants match the current search.");
            }
            applicantsEmptyLabel.setVisible(isEmpty);
            applicantsEmptyLabel.setManaged(isEmpty);
            applicantCardScroll.setVisible(!isEmpty);
            applicantCardScroll.setManaged(!isEmpty);
        }

        // Separate applicants by status
        List<ApplicantDisplay> hired = new java.util.ArrayList<>();
        List<ApplicantDisplay> pending = new java.util.ArrayList<>();
        List<ApplicantDisplay> rejected = new java.util.ArrayList<>();

        for (ApplicantDisplay applicant : filteredApplicants) {
            if (applicant.isHired()) {
                hired.add(applicant);
            } else if (applicant.getRecord().getStatus() == ApplicationStatus.REJECTED) {
                rejected.add(applicant);
            } else {
                pending.add(applicant);
            }
        }

        // Earliest apply time first within each group.
        Comparator<ApplicantDisplay> byApplyTimeAsc = Comparator.comparing(
                d -> d.getRecord().getApplyTime(),
                Comparator.nullsLast(java.time.LocalDateTime::compareTo));
        hired.sort(byApplyTimeAsc);
        rejected.sort(byApplyTimeAsc);

        if (isTopMatchActive && !topMatchApplicantIds.isEmpty()) {
            // TOP MATCH cards float to the top of the Pending block. Order amongst them is by
            // AI score (desc) with earliest apply-time as the tiebreaker. Everything else stays
            // in apply-time order at the bottom.
            pending.sort(Comparator
                    .comparingInt((ApplicantDisplay d) ->
                            topMatchApplicantIds.contains(normalizeTaId(d.getTaId())) ? 0 : 1)
                    .thenComparing(d -> top3Scores.getOrDefault(normalizeTaId(d.getTaId()), 0),
                            Comparator.reverseOrder())
                    .thenComparing(byApplyTimeAsc));
        } else {
            pending.sort(byApplyTimeAsc);
        }

        // Add separators between groups and render cards
        java.util.function.Consumer<List<ApplicantDisplay>> renderGroup = (applicants) -> {
            for (ApplicantDisplay applicant : applicants) {
                applicantCardPane.getChildren().add(createApplicantCard(applicant));
            }
        };

        boolean hasHired = !hired.isEmpty();
        boolean hasPending = !pending.isEmpty();
        boolean hasRejected = !rejected.isEmpty();

        // Render pending first (with AI Top 3 sorting applied).
        if (hasPending) {
            renderGroup.accept(pending);
        }

        // Add separator between pending and hired.
        if (hasPending && (hasHired || hasRejected)) {
            applicantCardPane.getChildren().add(createSeparator());
        }

        // Render hired second.
        if (hasHired) {
            renderGroup.accept(hired);
        }

        // Add separator between hired and rejected.
        if (hasHired && hasRejected) {
            applicantCardPane.getChildren().add(createSeparator());
        }

        // Render rejected last.
        if (hasRejected) {
            renderGroup.accept(rejected);
        }

        // Default selection: first Pending → first Hired → first Not Hired.
        if (selectedApplicant == null) {
            ApplicantDisplay defaultPick = null;
            if (!pending.isEmpty()) {
                defaultPick = pending.get(0);
            } else if (!hired.isEmpty()) {
                defaultPick = hired.get(0);
            } else if (!rejected.isEmpty()) {
                defaultPick = rejected.get(0);
            }
            if (defaultPick != null) {
                selectedApplicant = defaultPick;
                updateApplicantDetail(selectedApplicant);
                String selectedApplyId = selectedApplicant.getRecord() == null
                        ? null
                        : selectedApplicant.getRecord().getApplyId();
                for (javafx.scene.Node node : applicantCardPane.getChildren()) {
                    if (node.getUserData() instanceof ApplicantDisplay ad
                            && ad.getRecord() != null
                            && java.util.Objects.equals(ad.getRecord().getApplyId(), selectedApplyId)) {
                        node.getStyleClass().add("applicant-card-selected");
                    }
                }
            }
        }
    }

    private javafx.scene.layout.VBox createSeparator() {
        javafx.scene.layout.VBox separator = new javafx.scene.layout.VBox();
        separator.getStyleClass().add("applicant-group-separator");
        separator.setPrefHeight(2);
        separator.setMaxHeight(2);
        separator.setMinHeight(2);
        // Stretch the separator to the full width of the container.
        separator.prefWidthProperty().bind(applicantCardPane.widthProperty().subtract(24));
        separator.maxWidthProperty().bind(separator.prefWidthProperty());
        return separator;
    }

    private javafx.scene.layout.VBox createApplicantCard(ApplicantDisplay applicant) {
        javafx.scene.layout.VBox card = new javafx.scene.layout.VBox();
        card.setSpacing(8);
        card.getStyleClass().addAll("applicant-card");
        card.setUserData(applicant);
        // Two cards per row that grow with the container: (paneWidth - hgap - left/right padding) / 2.
        // hgap is 12; padding is roughly 12 on each side via .applicant-card-container.
        card.prefWidthProperty().bind(
                applicantCardPane.widthProperty().subtract(36).divide(2));
        card.maxWidthProperty().bind(card.prefWidthProperty());
        card.setMinWidth(220);

        if (applicant.isHired()) {
            card.getStyleClass().add("applicant-card-hired");
        }
        if (topMatchApplicantIds.contains(normalizeTaId(applicant.getTaId()))) {
            card.getStyleClass().add("applicant-card-recommended");
        }
        // Selected style is added LAST so it visually wins via CSS specificity.
        // Identify by applyId, not taId, so a single TA can never light up two cards.
        if (selectedApplicant != null
                && selectedApplicant.getRecord() != null
                && applicant.getRecord() != null
                && java.util.Objects.equals(
                        selectedApplicant.getRecord().getApplyId(),
                        applicant.getRecord().getApplyId())) {
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

        if (topMatchApplicantIds.contains(normalizeTaId(applicant.getTaId()))) {
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
                    Ta ta = applicant.getTa();
                    if (ta == null) continue;
                    String taId = applicant.getTaId();

                    // Skip if already in the in-memory cache for this session.
                    if (applicantSummaries.containsKey(taId)) continue;

                    // Reuse the AI summary stored on the TA record when present.
                    String cached = ta.getAiSummary();
                    if (cached != null && !cached.isBlank()) {
                        applicantSummaries.put(taId, cached.trim());
                        javafx.application.Platform.runLater(() -> renderApplicantCards());
                        continue;
                    }

                    // No persisted summary yet: generate a new one and persist it back to TA.csv.
                    try {
                        String summary = services.aiService().generateApplicantSummary(ta);
                        applicantSummaries.put(taId, summary);
                        ta.setAiSummary(summary);
                        services.profileService().updateTa(ta);
                        javafx.application.Platform.runLater(() -> renderApplicantCards());
                    } catch (Exception ex) {
                        // Fallback summary: leave the persisted column blank so the next run retries.
                        String fallback = ta.getMajor() + " major with skills in " +
                                (ta.getSkills() != null && !ta.getSkills().isBlank()
                                        ? ta.getSkills().split(",")[0] : "various areas");
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
    private void handleAiRecommendApplicants() {
        Job job = jobSelector.getSelectionModel().getSelectedItem();
        if (job == null) {
            DialogUtil.error("Please select a job first", navigator.getPrimaryStage());
            return;
        }
        List<ApplicantDisplay> allApplicants = currentApplicants();
        if (allApplicants.isEmpty()) {
            DialogUtil.error("No applicants for this job", navigator.getPrimaryStage());
            return;
        }
        List<ApplicantDisplay> pendingApplicants = allApplicants.stream()
                .filter(a -> a.getRecord().getStatus() == ApplicationStatus.PENDING)
                .collect(java.util.stream.Collectors.toList());
        if (pendingApplicants.isEmpty()) {
            DialogUtil.error("No pending applicants for this job", navigator.getPrimaryStage());
            return;
        }

        // Resolve the user-chosen Top-N (defaults to 3 when the spinner is missing/invalid).
        int topN = 3;
        if (aiTopCountSpinner != null && aiTopCountSpinner.getValue() != null) {
            topN = aiTopCountSpinner.getValue();
        }
        topN = Math.max(1, Math.min(10, topN));
        // Cap at the number of available pending applicants.
        final int requestedTopN = Math.min(topN, pendingApplicants.size());
        final String preference = aiPreferenceArea == null || aiPreferenceArea.getText() == null
                ? ""
                : aiPreferenceArea.getText().trim();

        aiKeywordsArea.setText("AI is selecting the top " + requestedTopN
                + " candidate(s) from pending applicants...");
        Task<List<AiService.ApplicantRecommendation>> task = new Task<>() {
            @Override
            protected List<AiService.ApplicantRecommendation> call() throws Exception {
                return services.aiService().recommendApplicantsForJob(
                        job, pendingApplicants, requestedTopN, preference);
            }
        };

        task.setOnSucceeded(evt -> {
            List<AiService.ApplicantRecommendation> recommendations = task.getValue();
            topMatchApplicantIds.clear();
            top3Scores.clear();

            // Build a normalized lookup so AI taIds with stray whitespace or different
            // casing still resolve to a real pending applicant.
            java.util.Map<String, ApplicantDisplay> pendingByNormalizedId = new java.util.HashMap<>();
            for (ApplicantDisplay d : pendingApplicants) {
                pendingByNormalizedId.put(normalizeTaId(d.getTaId()), d);
            }

            // Keep only AI rows that map to an actual pending applicant. Preserve AI's
            // ranking; deduplicate on the way in.
            java.util.LinkedHashMap<String, AiService.ApplicantRecommendation> validRecs =
                    new java.util.LinkedHashMap<>();
            for (AiService.ApplicantRecommendation rec : recommendations) {
                String key = normalizeTaId(rec.taId());
                if (pendingByNormalizedId.containsKey(key) && !validRecs.containsKey(key)) {
                    validRecs.put(key, rec);
                    top3Scores.put(key, rec.score());
                }
            }

            // We must mark exactly min(requestedTopN, pendingApplicants.size()) cards.
            // If the AI returned fewer valid hits than that, fill from the remaining
            // pending applicants (in apply-time order) at score 0 so they still surface.
            int targetCount = Math.min(requestedTopN, pendingApplicants.size());
            java.util.List<String> orderedKeys = new java.util.ArrayList<>(validRecs.keySet());
            if (orderedKeys.size() < targetCount) {
                List<ApplicantDisplay> apTime = new java.util.ArrayList<>(pendingApplicants);
                apTime.sort(Comparator.comparing(
                        d -> d.getRecord().getApplyTime(),
                        Comparator.nullsLast(java.time.LocalDateTime::compareTo)));
                for (ApplicantDisplay d : apTime) {
                    if (orderedKeys.size() >= targetCount) break;
                    String key = normalizeTaId(d.getTaId());
                    if (!validRecs.containsKey(key)) {
                        orderedKeys.add(key);
                        top3Scores.putIfAbsent(key, 0);
                    }
                }
            }
            for (int i = 0; i < Math.min(targetCount, orderedKeys.size()); i++) {
                topMatchApplicantIds.add(orderedKeys.get(i));
            }

            isTopMatchActive = true;
            // Re-pick the top-ranked card on the next render.
            selectedApplicant = null;

            filterApplicants(applicantSearchField.getText());

            StringBuilder sb = new StringBuilder(
                    "Top " + topMatchApplicantIds.size() + " candidate(s) (from pending):\n\n");
            int idx = 1;
            for (AiService.ApplicantRecommendation rec : validRecs.values()) {
                if (idx > targetCount) break;
                sb.append(String.format("%d. %s (Score: %d)%n   %s%n%n",
                        idx++, rec.taId(), rec.score(), rec.reason()));
            }
            if (validRecs.size() < targetCount) {
                sb.append("(AI returned only ")
                        .append(validRecs.size())
                        .append(" matches; remaining slots filled from the rest of the Pending pool.)\n");
            }
            aiKeywordsArea.setText(sb.toString());
        });

        task.setOnFailed(evt -> aiKeywordsArea.setText(
                "AI selection failed: " + task.getException().getMessage()));

        new Thread(task, "ai-recommend-applicants").start();
    }

    private void updateApplicantDetail(ApplicantDisplay display) {
        if (display == null || display.getTa() == null) {
            applicantNameLabel.setText("None selected");
            applicantStatusLabel.setText("-");
            applicantProfileArea.clear();
            updateActionButtons(null);
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
        updateActionButtons(display);
    }

    private void updateActionButtons(ApplicantDisplay display) {
        boolean isPending = display != null
                && display.getRecord() != null
                && display.getRecord().getStatus() == ApplicationStatus.PENDING;
        if (hireButton != null) {
            hireButton.setDisable(!isPending);
        }
        if (rejectButton != null) {
            rejectButton.setDisable(!isPending);
        }
    }

    @FXML
    private void handleHireApplicant() {
        if (selectedApplicant == null) {
            DialogUtil.error("Please select an applicant first", navigator.getPrimaryStage());
            return;
        }
        ApplicantDisplay display = selectedApplicant;
        ApplicationRecord record = display.getRecord();
        if (record.getStatus() != ApplicationStatus.PENDING) {
            DialogUtil.info("This applicant has already been decided.",
                    navigator.getPrimaryStage());
            return;
        }
        if (!DialogUtil.confirm("Hire this applicant?", navigator.getPrimaryStage())) {
            return;
        }
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
        OperationResult<Void> result = services.applicationService().hireApplicant(record.getApplyId());
        finishApplicantAction(result);
    }

    @FXML
    private void handleRejectApplicant() {
        if (selectedApplicant == null) {
            DialogUtil.error("Please select an applicant first", navigator.getPrimaryStage());
            return;
        }
        ApplicantDisplay display = selectedApplicant;
        ApplicationRecord record = display.getRecord();
        if (record.getStatus() != ApplicationStatus.PENDING) {
            DialogUtil.info("This applicant has already been decided.", navigator.getPrimaryStage());
            return;
        }
        OperationResult<Void> result = services.applicationService().rejectApplicant(record.getApplyId());
        finishApplicantAction(result);
    }

    /**
     * Shared post-action flow: report the outcome and reload the applicant list while keeping
     * the MO on the same job (no jump to the first job).
     */
    private void finishApplicantAction(OperationResult<Void> result) {
        if (result.success()) {
            DialogUtil.info(result.message(), navigator.getPrimaryStage());
            Job currentJob = jobSelector.getSelectionModel().getSelectedItem();
            String currentApplyId = selectedApplicant != null && selectedApplicant.getRecord() != null
                    ? selectedApplicant.getRecord().getApplyId()
                    : null;
            refreshMyJobs();
            loadApplicants(currentJob);
            // Try to keep the same applicant card selected so the MO can keep iterating.
            if (currentApplyId != null) {
                for (ApplicantDisplay candidate : applicantItems) {
                    if (candidate.getRecord() != null
                            && currentApplyId.equals(candidate.getRecord().getApplyId())) {
                        selectedApplicant = candidate;
                        updateApplicantDetail(candidate);
                        renderApplicantCards();
                        break;
                    }
                }
            }
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
            aiKeywordsArea.setText(String.join(", ", task.getValue()));
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
    private void handleGenerateJobKeywords() {
        String requirements = requirementsField.getText() == null ? "" : requirementsField.getText().trim();
        String notes = notesField.getText() == null ? "" : notesField.getText().trim();
        String moduleName = moduleField.getText() == null ? "" : moduleField.getText().trim();
        String jobName = jobNameField.getText() == null ? "" : jobNameField.getText().trim();

        if (jobName.isBlank() && requirements.isBlank() && notes.isBlank()) {
            DialogUtil.error(
                    "Please fill in at least one of Job title, Requirements, or Notes before generating keywords.",
                    navigator.getPrimaryStage());
            return;
        }

        generateKeywordsBtn.setDisable(true);
        keywordsLoadingLabel.setVisible(true);

        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() throws Exception {
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
            keywordsField.setText(String.join(", ", keywords));
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
