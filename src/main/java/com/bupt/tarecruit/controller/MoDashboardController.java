package com.bupt.tarecruit.controller;

import com.bupt.tarecruit.entity.*;
import com.bupt.tarecruit.service.AiService;
import com.bupt.tarecruit.service.ApplicationService;
import com.bupt.tarecruit.util.DateTimeUtil;
import com.bupt.tarecruit.util.DialogUtil;
import com.bupt.tarecruit.util.FileStorageHelper;
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
 * Controller for the MO (and admin) dashboard.
 * <p>
 * MO users manage jobs, review applicants (hire/reject, AI ranking, similar candidates),
 * and edit profile. Admin users gain extra tabs: TA/MO accounts, all jobs, insights,
 * account logs, and job management logs.
 * </p>
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
    /** Top-match ranking for the current job; survives applicant list reload after hire/reject. */
    private final ApplicantAiHighlightState aiHighlights = new ApplicantAiHighlightState();
    /** Blocks job-selector listener while jobOptions is being refreshed (avoids spurious loadApplicants(null)). */
    private boolean suppressJobSelectorApplicantReload;

    /** Root tab pane for MO workflow and optional admin tabs. */
    @FXML
    private TabPane tabPane;

    /** Applicant review tab for the selected job. */
    @FXML
    private Tab applicantsTab;

    /** List of jobs owned by the current MO. */
    @FXML
    private Tab myJobsTab;

    /** Create/edit job form tab. */
    @FXML
    private Tab postEditJobTab;

    /** Welcome header with display name. */
    @FXML
    private Label welcomeLabel;

    /** Filter for the my-jobs table (applied on refresh). */
    @FXML
    private TextField myJobSearchField;

    /** MO's jobs with selection driving detail panel. */
    @FXML
    private TableView<Job> myJobTable;

    /** Detail: selected job title. */
    @FXML
    private Label selectedJobNameLabel;

    /** Detail: open/closed status. */
    @FXML
    private Label selectedJobStatusLabel;

    /** Detail: start and end dates. */
    @FXML
    private Label selectedJobDatesLabel;

    /** Detail: active applicant count. */
    @FXML
    private Label selectedJobApplicantsLabel;

    /** Detail: hired count. */
    @FXML
    private Label selectedJobHiredLabel;

    /** Detail: requirements text. */
    @FXML
    private TextArea selectedJobRequirementsArea;

    /** Detail: additional notes. */
    @FXML
    private TextArea selectedJobNotesArea;

    /** Detail: keyword list. */
    @FXML
    private TextArea selectedJobKeywordsArea;

    /** Post/edit form: job title. */
    @FXML
    private TextField jobNameField;

    /** Post/edit form: module name. */
    @FXML
    private TextField moduleField;

    /** Post/edit form: number of positions. */
    @FXML
    private Spinner<Integer> positionsSpinner;

    /** Post/edit form: start date. */
    @FXML
    private DatePicker startDatePicker;

    /** Post/edit form: end date. */
    @FXML
    private DatePicker endDatePicker;

    /** Post/edit form: requirements. */
    @FXML
    private TextArea requirementsField;

    /** Post/edit form: comma-separated keywords. */
    @FXML
    private TextArea keywordsField;

    /** Triggers AI keyword generation from the post/edit form. */
    @FXML
    private Button generateKeywordsBtn;

    /** Shown while form keyword generation runs. */
    @FXML
    private Label keywordsLoadingLabel;

    /** Post/edit form: additional notes. */
    @FXML
    private TextArea notesField;

    /** Inline status after save job. */
    @FXML
    private Label formStatusLabel;

    /** Shows new vs edit job id in the form header. */
    @FXML
    private Label formJobIdLabel;

    /** Selects which job's applicants are shown. */
    @FXML
    private ComboBox<Job> jobSelector;

    /** Applicant search (applied on Refresh). */
    @FXML
    private TextField applicantSearchField;

    /** Legacy table binding; cards are primary UI. */
    @FXML
    private TableView<ApplicantDisplay> applicantTable;

    /** Flow layout of applicant summary cards. */
    @FXML
    private FlowPane applicantCardPane;

    /** Scroll container for applicant cards. */
    @FXML
    private ScrollPane applicantCardScroll;

    /** Detail panel: applicant name and id. */
    @FXML
    private Label applicantNameLabel;

    /** Detail panel: application status. */
    @FXML
    private Label applicantStatusLabel;

    /** Detail panel: full profile text. */
    @FXML
    private TextArea applicantProfileArea;

    /** AI applicant ranking narrative (top matches / similar search). */
    @FXML
    private TextArea aiApplicantResultArea;

    /** AI keyword output or ranking status for applicants tab. */
    @FXML
    private TextArea aiKeywordsArea;

    /** MO free-text preference for AI applicant ranking. */
    @FXML
    private TextArea aiPreferenceArea;

    /** How many top pending applicants AI should return (1–10). */
    @FXML
    private Spinner<Integer> aiTopCountSpinner;

    /** Hires the card-selected pending applicant. */
    @FXML
    private Button hireButton;

    /** Rejects the card-selected pending applicant. */
    @FXML
    private Button rejectButton;

    /** MO profile: full name. */
    @FXML
    private TextField moFullNameField;

    /** MO profile: phone. */
    @FXML
    private TextField moPhoneField;

    /** MO profile: email (read-only). */
    @FXML
    private TextField moEmailField;

    /** MO profile: responsible modules. */
    @FXML
    private TextArea moModuleArea;

    /** Parent admin tab (legacy; may be unused in FXML). */
    @FXML
    private Tab adminUserTab;

    /** Admin: TA account management tab. */
    @FXML
    private Tab adminTaTab;

    /** Admin: MO account management tab. */
    @FXML
    private Tab adminMoTab;

    /** Admin: all jobs tab. */
    @FXML
    private Tab adminJobTab;

    /** Admin: embedded insights tab. */
    @FXML
    private Tab adminInsightsTab;

    /** Admin: account audit log tab. */
    @FXML
    private Tab adminAccountTab;

    /** Admin: job open/close audit log tab. */
    @FXML
    private Tab adminJobManagementTab;

    /** Right-hand applicant detail column. */
    @FXML
    private javafx.scene.layout.VBox applicantDetailPanel;

    /** Right-hand my-jobs detail column. */
    @FXML
    private javafx.scene.layout.VBox myJobsDetailPanel;

    /** Split between applicant cards and detail. */
    @FXML
    private SplitPane applicantsSplitPane;

    /** Split within applicant detail (profile vs AI). */
    @FXML
    private SplitPane applicantDetailSplit;

    /** Split for my-jobs list vs detail. */
    @FXML
    private SplitPane myJobsSplitPane;

    /** Split on admin TA master/detail. */
    @FXML
    private SplitPane adminTaSplitPane;

    /** Split on admin MO master/detail. */
    @FXML
    private SplitPane adminMoSplitPane;

    /** Left stack for applicant cards or empty state. */
    @FXML
    private javafx.scene.layout.StackPane applicantsLeftPane;

    /** Shown when the selected job has no applicants. */
    @FXML
    private Label applicantsEmptyLabel;

    /** Nested controller for admin insights metrics and AI section. */
    @FXML
    private InsightsDialogController insightsViewController;

    /** Guards one-time insights wiring when the admin insights tab is first opened. */
    private boolean insightsLoaded;

    /** Legacy TA user table (admin). */
    @FXML
    private TableView<Ta> taUserTable;

    /** Legacy MO user table (admin). */
    @FXML
    private TableView<Mo> moUserTable;

    /** Admin table of all jobs with hired counts. */
    @FXML
    private TableView<AdminJobDisplay> adminJobTable;

    /** Admin TA list with workload highlighting. */
    @FXML
    private TableView<AdminTaDisplay> adminTaTable;

    /** Admin MO list. */
    @FXML
    private TableView<AdminMoDisplay> adminMoTable;

    /** Jobs the selected TA has applied to. */
    @FXML
    private TableView<AdminTaDisplay.JobApplicationInfo> adminTaAppliedJobsTable;

    /** Jobs the selected TA was hired for. */
    @FXML
    private TableView<AdminTaDisplay.JobApplicationInfo> adminTaHiredJobsTable;

    /** Jobs posted by the selected MO in admin detail. */
    @FXML
    private TableView<AdminJobDisplay> adminMoJobsTable;

    /** Account management audit log. */
    @FXML
    private TableView<AccountLogDisplay> accountLogTable;

    /** Job open/close audit log. */
    @FXML
    private TableView<JobLogDisplay> jobLogTable;

    /** Admin TA detail: email header. */
    @FXML
    private Label adminTaNameLabel;

    /** Admin TA detail: name/phone/major. */
    @FXML
    private Label adminTaInfoLabel;

    /** Admin MO detail: email header. */
    @FXML
    private Label adminMoNameLabel;

    /** Admin MO detail: name/phone/modules. */
    @FXML
    private Label adminMoInfoLabel;

    /**
     * Wires tables, filters, split panes, job selector, applicant cards, and admin tab refresh hooks.
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
            if (suppressJobSelectorApplicantReload) {
                return;
            }
            // ComboBox.setAll() briefly clears selection; ignore null, not a real job change.
            if (val == null) {
                return;
            }
            if (old != null && sameJobId(old.getJobId(), val.getJobId())) {
                return;
            }
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
        // Avoid stealing focus from applicant cards after hire/confirm dialogs.
        if (aiPreferenceArea != null) {
            aiPreferenceArea.setFocusTraversable(false);
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

    /**
     * Binds the MO or admin session, toggles admin tabs, selects the initial tab, and loads data.
     *
     * @param session authenticated MO or admin session
     */
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

        suppressJobSelectorApplicantReload = true;
        try {
            List<Job> jobs = services.jobService().findJobsByMo(currentMoId());
            jobs.sort(Comparator.comparing(Job::getJobId));
            myJobs.setAll(jobs);
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
                jobSelector.getSelectionModel().clearSelection();
                aiHighlights.clear();
                reloadApplicantItems(null);
                selectedApplicant = null;
                clearApplicantsAiOutputs();
                renderApplicantCards();
            }
        } finally {
            suppressJobSelectorApplicantReload = false;
        }
        // selectFirst()/select() above runs while suppressed, so the listener never calls
        // loadApplicants — reload here on first paint when the list is still empty.
        Job selected = jobSelector.getSelectionModel().getSelectedItem();
        if (selected != null && applicantItems.isEmpty()) {
            loadApplicants(selected);
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
        if (job == null) {
            aiHighlights.clear();
            applicantSummaries.clear();
            selectedApplicant = null;
            applicantItems.clear();
            renderApplicantCards();
            return;
        }
        if (!aiHighlights.isForJob(job.getJobId())) {
            aiHighlights.clear();
        }
        applicantSummaries.clear();
        selectedApplicant = null;
        reloadApplicantItems(job);
        filteredApplicants.setPredicate(item -> true);
        aiHighlights.restoreAnalysisTo(aiKeywordsArea);
        renderApplicantCards();
        generateAllSummariesInBackground();
    }

    private void reloadApplicantItems(Job job) {
        if (job == null) {
            applicantItems.clear();
            return;
        }
        List<Ta> allTa = services.adminService().findAllTa();
        Map<String, Ta> taMap = allTa.stream().collect(Collectors.toMap(Ta::getTaId, Function.identity(), (a, b) -> a));
        ApplicationService applicationService = services.applicationService();
        applicantItems.setAll(applicationService.findActiveApplicationsForJob(job.getJobId()).stream()
                .map(record -> new ApplicantDisplay(record, taMap.get(record.getTaId())))
                .filter(display -> !(display.getRecord().getStatus() == ApplicationStatus.PENDING
                        && display.getTa() != null
                        && display.getTa().isDisabled()))
                .collect(Collectors.toList()));

        for (ApplicantDisplay applicant : applicantItems) {
            Ta ta = applicant.getTa();
            if (ta == null) {
                continue;
            }
            String cached = ta.getAiSummary();
            if (cached != null && !cached.isBlank()) {
                applicantSummaries.putIfAbsent(applicant.getTaId(), cached.trim());
            }
        }
    }

    private void refreshApplicantsAfterHireOrReject(Job job, String decidedTaId) {
        if (job == null) {
            return;
        }
        reloadApplicantItems(job);
        if (aiHighlights.isForJob(job.getJobId())) {
            java.util.Set<String> pendingIds = applicantItems.stream()
                    .filter(d -> d.getRecord() != null
                            && d.getRecord().getStatus() == ApplicationStatus.PENDING)
                    .map(d -> normalizeTaId(d.getTaId()))
                    .collect(Collectors.toSet());
            aiHighlights.afterApplicantDecided(decidedTaId, pendingIds);
            aiHighlights.restoreAnalysisTo(aiKeywordsArea);
        }
        selectNextApplicantForReview();
        filterApplicants(applicantSearchField == null ? "" : applicantSearchField.getText());
    }

    private void selectNextApplicantForReview() {
        Job job = jobSelector.getSelectionModel().getSelectedItem();
        String jobId = job != null ? job.getJobId() : "";
        if (aiHighlights.isActive() && aiHighlights.isForJob(jobId)) {
            Optional<ApplicantDisplay> nextTop = applicantItems.stream()
                    .filter(d -> d.getRecord() != null
                            && d.getRecord().getStatus() == ApplicationStatus.PENDING
                            && aiHighlights.contains(d.getTaId()))
                    .max(Comparator
                            .comparingInt((ApplicantDisplay d) -> aiHighlights.scoreOf(d.getTaId()))
                            .thenComparing(
                                    d -> d.getRecord().getApplyTime(),
                                    Comparator.nullsLast(java.time.LocalDateTime::compareTo)));
            if (nextTop.isPresent()) {
                selectedApplicant = nextTop.get();
                updateApplicantDetail(selectedApplicant);
                return;
            }
        }
        applicantItems.stream()
                .filter(d -> d.getRecord() != null
                        && d.getRecord().getStatus() == ApplicationStatus.PENDING)
                .min(Comparator.comparing(
                        d -> d.getRecord().getApplyTime(),
                        Comparator.nullsLast(java.time.LocalDateTime::compareTo)))
                .ifPresentOrElse(next -> {
                    selectedApplicant = next;
                    updateApplicantDetail(next);
                }, () -> selectedApplicant = null);
    }

    private static boolean sameJobId(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return a.equalsIgnoreCase(b);
    }

    /** Clears applicant AI highlights, cancels ranking tasks, and re-renders cards. */
    @FXML
    private void handleResetAiRecommendations() {
        if (activeRecommendApplicantsTask != null && activeRecommendApplicantsTask.isRunning()) {
            activeRecommendApplicantsTask.cancel(true);
        }
        if (activeSimilarApplicantsTask != null && activeSimilarApplicantsTask.isRunning()) {
            activeSimilarApplicantsTask.cancel(true);
        }
        aiHighlights.clear();
        clearApplicantsAiOutputs();
        selectedApplicant = null;
        filterApplicants(applicantSearchField == null ? "" : applicantSearchField.getText());
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

    /** Applies the my-jobs search filter. */
    @FXML
    private void handleRefreshMyJobs() {
        filterJobs(myJobSearchField.getText());
    }

    /** Loads the selected job into the post/edit form and switches to that tab. */
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

    /** Clears the post/edit form for a new job posting. */
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

    /** Creates or updates the job via {@link com.bupt.tarecruit.service.JobService#upsertJob}. */
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

    /** Closes the selected job, rejects pending applications, and writes a job log entry. */
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
                log.setLogId(generateJobLogId());
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

    /** Re-opens a closed job from my-jobs and logs the action. */
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
                log.setLogId(generateJobLogId());
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

    /** Selects an open job in the applicant combo and switches to the applicants tab. */
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

    /** Re-applies applicant search filter and re-renders cards. */
    @FXML
    private void handleRefreshApplicants() {
        // Apply current search filter; this is the only way to trigger search.
        filterApplicants(applicantSearchField.getText());
    }

    /** Legacy handler: clears applicant search (may be unused in FXML). */
    @FXML
    private void handleClearApplicantSearch() {
        // Retained for backwards compatibility; no longer wired into the FXML.
        applicantSearchField.clear();
        filterApplicants(null);
    }

    /** Switches to the admin insights tab (data loads on first visit). */
    @FXML
    private void handleAiGenerateInsights() {
        // Insights now live as their own admin tab; selecting that tab triggers loading.
        if (tabPane != null && adminInsightsTab != null) {
            tabPane.getSelectionModel().select(adminInsightsTab);
        }
    }

    /** Saves MO profile fields (name, phone, modules). */
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

    /** Opens the change-password dialog for the signed-in MO. */
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

    /** Returns to the MO portal login screen. */
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

    /** Admin: resets password for the selected TA. */
    @FXML
    private void handleResetTaPassword() {
        AdminTaDisplay display = adminTaTable != null ? adminTaTable.getSelectionModel().getSelectedItem() : null;
        Ta ta = display != null ? display.getTa() : null;
        if (ta == null && taUserTable != null) {
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

    /** Admin: enables or disables the selected TA account. */
    @FXML
    private void handleToggleTaStatus() {
        AdminTaDisplay display = adminTaTable != null ? adminTaTable.getSelectionModel().getSelectedItem() : null;
        Ta ta = display != null ? display.getTa() : null;
        if (ta == null && taUserTable != null) {
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

    /** Admin: resets password for the selected MO. */
    @FXML
    private void handleResetMoPassword() {
        AdminMoDisplay display = adminMoTable != null ? adminMoTable.getSelectionModel().getSelectedItem() : null;
        Mo mo = display != null ? display.getMo() : null;
        if (mo == null && moUserTable != null) {
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

    /** Admin: enables or disables the selected MO account. */
    @FXML
    private void handleToggleMoStatus() {
        AdminMoDisplay display = adminMoTable != null ? adminMoTable.getSelectionModel().getSelectedItem() : null;
        Mo mo = display != null ? display.getMo() : null;
        if (mo == null && moUserTable != null) {
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

    /** Admin: closes the selected job in the all-jobs table and logs the action. */
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
            log.setLogId(generateJobLogId());
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

    /** Admin: re-opens the selected closed job and logs the action. */
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
            log.setLogId(generateJobLogId());
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

    /** Placeholder; job management UI moved to the all-jobs admin tab. */
    @FXML
    private void handleToggleJobManagementDisabled() {
        // Removed - job management is done in All Jobs tab
    }

    /** Placeholder refresh for legacy job-management tab. */
    @FXML
    private void handleRefreshJobManagement() {
        // Removed - job management is done in All Jobs tab
    }

    /** Reloads job open/close audit log rows. */
    @FXML
    private void handleRefreshJobLogs() {
        refreshJobLogs();
    }

    /** Admin: wipes AccountLogs.csv after confirmation. */
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

    /** Admin: wipes JobLogs.csv after confirmation. */
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

    private String generateJobLogId() {
        List<String> existing = services.jobLogDao().findAll().stream()
                .map(JobLog::getLogId)
                .collect(Collectors.toList());
        return com.bupt.tarecruit.util.IdGenerator.nextId("jlog", existing);
    }

    /** Admin: reloads account management audit log table. */
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

        Job currentJob = jobSelector.getSelectionModel().getSelectedItem();
        String currentJobId = currentJob != null ? currentJob.getJobId() : "";
        boolean aiRankingActive = aiHighlights.isActive() && aiHighlights.isForJob(currentJobId);
        if (aiRankingActive) {
            pending.sort(Comparator
                    .comparingInt((ApplicantDisplay d) -> aiHighlights.contains(d.getTaId()) ? 0 : 1)
                    .thenComparing((ApplicantDisplay d) -> aiHighlights.scoreOf(d.getTaId()),
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
        if (aiHighlights.contains(applicant.getTaId())) {
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

        if (aiHighlights.contains(applicant.getTaId())) {
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

                    // Reuse the AI summary stored on the TA record when present and still valid.
                    String cached = ta.getAiSummary();
                    if (cached != null && !cached.isBlank()) {
                        applicantSummaries.put(taId, cached.trim());
                        javafx.application.Platform.runLater(() -> renderApplicantCards());
                        continue;
                    }

                    String cvText = readAttachedCvText(ta);
                    // No persisted summary yet: generate and persist to TA.csv.
                    try {
                        String summary = services.aiService().generateApplicantSummary(ta, cvText);
                        applicantSummaries.put(taId, summary);
                        ta.setAiSummary(summary);
                        services.profileService().updateTa(ta);
                        javafx.application.Platform.runLater(() -> renderApplicantCards());
                    } catch (Exception ex) {
                        String fallback = AiService.fallbackApplicantSummary(ta, cvText);
                        applicantSummaries.put(taId, fallback);
                        javafx.application.Platform.runLater(() -> renderApplicantCards());
                    }
                }
                return null;
            }
        };
        new Thread(task, "applicant-summary-generator").start();
    }

    /**
     * AI-ranks pending applicants for the selected job (respects Top-N spinner and MO preference),
     * highlights cards, and shows scored reasons.
     */
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

        task.setOnSucceeded(evt -> applyApplicantRecommendationResults(
                task.getValue(),
                pendingApplicants,
                requestedTopN,
                "Top %d candidate(s) (from pending):"));

        task.setOnFailed(evt -> aiKeywordsArea.setText(
                "AI selection failed: " + task.getException().getMessage()));

        activeRecommendApplicantsTask = task;
        new Thread(task, "ai-recommend-applicants").start();
    }

    /**
     * After the MO completes a hire attempt (hired or declined concurrent-job warning),
     * optionally run AI similar-candidate recommendations based on that applicant.
     */
    private void promptRecommendSimilarAfterHire(ApplicantDisplay benchmarkDisplay) {
        if (benchmarkDisplay == null || benchmarkDisplay.getTa() == null) {
            return;
        }
        Ta benchmark = benchmarkDisplay.getTa();
        String applicantLabel = benchmark.getDisplayLabel() + " (" + benchmark.getTaId() + ")";
        Optional<Integer> choice = DialogUtil.confirmRecommendSimilarCandidates(
                applicantLabel, navigator.getPrimaryStage());
        if (choice.isEmpty()) {
            return;
        }
        int topN = choice.get();
        if (aiTopCountSpinner != null) {
            aiTopCountSpinner.getValueFactory().setValue(topN);
        }
        runSimilarApplicantRecommendation(benchmarkDisplay, topN);
    }

    private void runSimilarApplicantRecommendation(ApplicantDisplay benchmarkDisplay, int topN) {
        Job job = jobSelector.getSelectionModel().getSelectedItem();
        if (job == null) {
            DialogUtil.error("Please select a job first", navigator.getPrimaryStage());
            return;
        }
        Ta benchmark = benchmarkDisplay.getTa();
        if (benchmark == null) {
            return;
        }
        String benchmarkKey = normalizeTaId(benchmark.getTaId());
        List<ApplicantDisplay> pendingApplicants = currentApplicants().stream()
                .filter(a -> a.getRecord() != null
                        && a.getRecord().getStatus() == ApplicationStatus.PENDING)
                .filter(a -> !normalizeTaId(a.getTaId()).equals(benchmarkKey))
                .collect(Collectors.toList());
        if (pendingApplicants.isEmpty()) {
            DialogUtil.error("No other pending applicants to recommend", navigator.getPrimaryStage());
            return;
        }

        final int requestedTopN = Math.min(Math.max(1, Math.min(10, topN)), pendingApplicants.size());
        final String benchmarkTaId = benchmark.getTaId();
        aiKeywordsArea.setText("AI is finding " + requestedTopN
                + " candidate(s) similar to " + benchmarkTaId + "...");

        Task<List<AiService.ApplicantRecommendation>> task = new Task<>() {
            @Override
            protected List<AiService.ApplicantRecommendation> call() throws Exception {
                return services.aiService().findSimilarApplicants(
                        job, benchmark, pendingApplicants, requestedTopN);
            }
        };

        task.setOnSucceeded(evt -> applyApplicantRecommendationResults(
                task.getValue(),
                pendingApplicants,
                requestedTopN,
                "Top %d similar candidate(s) (based on " + benchmarkTaId + "):"));

        task.setOnFailed(evt -> aiKeywordsArea.setText(
                "AI similar-candidate search failed: " + task.getException().getMessage()));

        if (activeSimilarApplicantsTask != null && activeSimilarApplicantsTask.isRunning()) {
            activeSimilarApplicantsTask.cancel(true);
        }
        activeSimilarApplicantsTask = task;
        new Thread(task, "ai-similar-applicants").start();
    }

    private void applyApplicantRecommendationResults(
            List<AiService.ApplicantRecommendation> recommendations,
            List<ApplicantDisplay> pendingApplicants,
            int requestedTopN,
            String resultHeaderFormat) {
        Job job = jobSelector.getSelectionModel().getSelectedItem();
        String jobId = job != null ? job.getJobId() : "";

        java.util.Map<String, ApplicantDisplay> pendingByNormalizedId = new java.util.HashMap<>();
        for (ApplicantDisplay d : pendingApplicants) {
            pendingByNormalizedId.put(normalizeTaId(d.getTaId()), d);
        }

        java.util.LinkedHashMap<String, AiService.ApplicantRecommendation> validRecs =
                new java.util.LinkedHashMap<>();
        java.util.Map<String, Integer> scoreByTaId = new java.util.HashMap<>();
        if (recommendations != null) {
            for (AiService.ApplicantRecommendation rec : recommendations) {
                String key = normalizeTaId(rec.taId());
                if (pendingByNormalizedId.containsKey(key) && !validRecs.containsKey(key)) {
                    validRecs.put(key, rec);
                    scoreByTaId.put(key, rec.score());
                }
            }
        }

        int targetCount = Math.min(requestedTopN, pendingApplicants.size());
        java.util.List<String> orderedKeys = new java.util.ArrayList<>(validRecs.keySet());
        if (orderedKeys.size() < targetCount) {
            List<ApplicantDisplay> apTime = new java.util.ArrayList<>(pendingApplicants);
            apTime.sort(Comparator.comparing(
                    d -> d.getRecord().getApplyTime(),
                    Comparator.nullsLast(java.time.LocalDateTime::compareTo)));
            for (ApplicantDisplay d : apTime) {
                if (orderedKeys.size() >= targetCount) {
                    break;
                }
                String key = normalizeTaId(d.getTaId());
                if (!validRecs.containsKey(key)) {
                    orderedKeys.add(key);
                    scoreByTaId.putIfAbsent(key, 0);
                }
            }
        }
        java.util.List<String> topIds = new java.util.ArrayList<>();
        for (int i = 0; i < Math.min(targetCount, orderedKeys.size()); i++) {
            topIds.add(orderedKeys.get(i));
        }

        StringBuilder sb = new StringBuilder(
                String.format(resultHeaderFormat, topIds.size()) + "\n\n");
        int idx = 1;
        for (AiService.ApplicantRecommendation rec : validRecs.values()) {
            if (idx > targetCount) {
                break;
            }
            sb.append(String.format("%d. %s (Score: %d)%n   %s%n%n",
                    idx++, rec.taId(), rec.score(), rec.reason()));
        }
        if (validRecs.size() < targetCount) {
            sb.append("(AI returned only ")
                    .append(validRecs.size())
                    .append(" matches; remaining slots filled from the rest of the Pending pool.)\n");
        }

        aiHighlights.applyRanking(jobId, topIds, scoreByTaId, sb.toString());
        if (aiKeywordsArea != null) {
            aiKeywordsArea.setText(sb.toString());
        }
        selectedApplicant = null;
        filterApplicants(applicantSearchField == null ? "" : applicantSearchField.getText());
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

    /**
     * Hires the selected pending applicant with concurrent-job warning and optional
     * similar-candidate AI prompt afterward.
     */
    @FXML
    private void handleHireApplicant() {
        if (selectedApplicant == null) {
            DialogUtil.error("Please select an applicant first", navigator.getPrimaryStage());
            return;
        }
        ApplicantDisplay hireTarget = selectedApplicant;
        ApplicationRecord record = hireTarget.getRecord();
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
                promptRecommendSimilarAfterHire(hireTarget);
                return;
            }
        }
        OperationResult<Void> result = services.applicationService().hireApplicant(record.getApplyId());
        finishApplicantAction(result, hireTarget, true);
    }

    /** Rejects the selected pending applicant and refreshes the applicant list for the same job. */
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
        finishApplicantAction(result, display, false);
    }

    /**
     * Shared post-action flow: report the outcome and reload the applicant list while keeping
     * the MO on the same job (no jump to the first job).
     */
    private void finishApplicantAction(
            OperationResult<Void> result, ApplicantDisplay decidedApplicant, boolean promptSimilar) {
        if (result.success()) {
            DialogUtil.info(result.message(), navigator.getPrimaryStage());
            String decidedTaId = decidedApplicant != null && decidedApplicant.getTa() != null
                    ? decidedApplicant.getTa().getTaId()
                    : null;
            String jobIdSnapshot = decidedApplicant != null && decidedApplicant.getRecord() != null
                    ? decidedApplicant.getRecord().getJobId()
                    : null;
            refreshMyJobs();
            Job currentJob = jobSelector.getSelectionModel().getSelectedItem();
            if (currentJob != null && jobIdSnapshot != null
                    && sameJobId(jobIdSnapshot, currentJob.getJobId())) {
                refreshApplicantsAfterHireOrReject(currentJob, decidedTaId);
            }
            if (promptSimilar && decidedApplicant != null) {
                promptRecommendSimilarAfterHire(decidedApplicant);
            }
        } else {
            DialogUtil.error(result.message(), navigator.getPrimaryStage());
        }
    }

    private String readAttachedCvText(Ta ta) {
        if (ta == null || ta.getCvPath() == null || ta.getCvPath().isBlank()) {
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

    /** Downloads the selected applicant's CV to a user-chosen file. */
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
        fileChooser.setInitialFileName(FileStorageHelper.cvFileName(ta.getTaId()));
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

    /** Generates quick-review keywords for the job selected in the applicants tab. */
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

    /** Generates keywords from the post/edit job form fields into {@link #keywordsField}. */
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

    /**
     * AI top-match highlights for one job. Kept separate from applicant list reload so hire/reject
     * can refresh CSV data without losing MO-visible ranking.
     */
    private static final class ApplicantAiHighlightState {
        private String jobId = "";
        private final java.util.LinkedHashSet<String> topMatchTaIds = new java.util.LinkedHashSet<>();
        private final java.util.Map<String, Integer> scores = new java.util.HashMap<>();
        private String analysisText = "";

        boolean isActive() {
            return !topMatchTaIds.isEmpty();
        }

        boolean isForJob(String id) {
            return id != null && !id.isBlank() && jobId != null && jobId.equalsIgnoreCase(id);
        }

        void clear() {
            jobId = "";
            topMatchTaIds.clear();
            scores.clear();
            analysisText = "";
        }

        void applyRanking(
                String jobId,
                java.util.List<String> orderedTaIds,
                java.util.Map<String, Integer> scoreByTaId,
                String analysis) {
            this.jobId = jobId == null ? "" : jobId;
            topMatchTaIds.clear();
            scores.clear();
            for (String taId : orderedTaIds) {
                String key = MoDashboardController.normalizeTaId(taId);
                topMatchTaIds.add(key);
                Integer score = scoreByTaId == null ? null : scoreByTaId.get(key);
                if (score != null) {
                    scores.put(key, score);
                }
            }
            analysisText = analysis == null ? "" : analysis;
        }

        void afterApplicantDecided(String decidedTaId, java.util.Set<String> pendingNormalizedIds) {
            if (decidedTaId != null) {
                String key = MoDashboardController.normalizeTaId(decidedTaId);
                topMatchTaIds.remove(key);
                scores.remove(key);
            }
            topMatchTaIds.retainAll(pendingNormalizedIds);
            scores.keySet().retainAll(topMatchTaIds);
        }

        boolean contains(String taId) {
            return topMatchTaIds.contains(MoDashboardController.normalizeTaId(taId));
        }

        int scoreOf(String taId) {
            return scores.getOrDefault(MoDashboardController.normalizeTaId(taId), 0);
        }

        void restoreAnalysisTo(TextArea area) {
            if (area != null && !analysisText.isBlank()) {
                area.setText(analysisText);
            }
        }
    }
}
