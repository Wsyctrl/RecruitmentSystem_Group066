package com.bupt.tarecruit.controller;

import com.bupt.tarecruit.entity.ApplicationRecord;
import com.bupt.tarecruit.entity.ApplicationStatus;
import com.bupt.tarecruit.entity.Job;
import com.bupt.tarecruit.entity.Mo;
import com.bupt.tarecruit.entity.Role;
import com.bupt.tarecruit.entity.Ta;
import com.bupt.tarecruit.entity.UserSession;
import com.bupt.tarecruit.util.PortalMode;
import com.bupt.tarecruit.viewmodel.AdminJobDisplay;
import com.bupt.tarecruit.viewmodel.AdminMoDisplay;
import com.bupt.tarecruit.viewmodel.AdminTaDisplay;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testfx.util.WaitForAsyncUtils;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TestFX UI tests for MoDashboardController MO and admin tabs.
 */
class MoDashboardControllerUiTest extends BaseUiTest {

    /** @return MO portal mode under test */
    @Override
    protected PortalMode getPortalMode() {
        return PortalMode.MO_PORTAL;
    }

    /** Opens the MO login screen before each test. */
    @Override
    protected void navigateInitialView() {
        navigator.showLogin();
    }

    /** Extends base seed with an extra TA and a default job for Bob. */
    @Override
    protected void seedData() {
        super.seedData();
        // Add another TA so there's a clear data set for admin/applicant tests.
        services.authService().register(Role.TA, "ta.daniel@bupt.edu.cn", "pass1234", "pass1234");
        services.profileService().findTa("ta.daniel@bupt.edu.cn").ifPresent(ta -> {
            ta.setFullName("Daniel");
            ta.setMajor("EE");
            ta.setSkills("Networking");
            ta.setExperience("3y");
            ta.setSelfEvaluation("Reliable");
            services.profileService().updateTa(ta);
        });

        // Pre-create a job for Bob so the tab has data.
        addJob("Initial Job", "InitialMod", 2, "REQ", "NOTES", LocalDate.now(), LocalDate.now().plusDays(30));
    }

    /**
     * Creates and persists a job owned by Bob for UI fixtures.
     *
     * @return saved job from {@link com.bupt.tarecruit.service.JobService#upsertJob}
     */
    private Job addJob(String name, String module, int positions, String req, String notes,
                       LocalDate start, LocalDate end) {
        Job j = new Job();
        j.setJobName(name);
        j.setModuleName(module);
        j.setNumberOfPositions(positions);
        j.setRequirements(req);
        j.setAdditionalNotes(notes);
        j.setStartDate(start);
        j.setEndDate(end);
        j.setMoId("mo.bob@bupt.edu.cn");
        return services.jobService().upsertJob(j).data();
    }

    /** Navigates to the MO dashboard as Bob. */
    private void loginAsBob() {
        runOnFx(() -> {
            UserSession session = new UserSession(Role.MO, null,
                    services.profileService().findMo("mo.bob@bupt.edu.cn").orElseThrow());
            navigator.showDashboard(session);
        });
        WaitForAsyncUtils.waitForFxEvents();
    }

    /** Navigates to the admin dashboard. */
    private void loginAsAdmin() {
        runOnFx(() -> {
            UserSession session = new UserSession(Role.ADMIN, null,
                    services.profileService().findMo("admin@bupt.edu.cn").orElseThrow());
            navigator.showDashboard(session);
        });
        WaitForAsyncUtils.waitForFxEvents();
    }

    /**
     * Selects a tab by its display text on the root tab pane.
     *
     * @param text tab label to select
     */
    private void selectTab(String text) {
        runOnFx(() -> {
            TabPane tabs = fx("#tabPane");
            for (Tab t : tabs.getTabs()) {
                if (text.equals(t.getText())) {
                    tabs.getSelectionModel().select(t);
                    return;
                }
            }
        });
        WaitForAsyncUtils.waitForFxEvents();
    }

    /**
     * @param text exact button label
     * @return first matching button in the selected tab or scene root
     */
    private Button findButtonByText(String text) {
        TabPane tabs = fx("#tabPane");
        if (tabs != null && tabs.getSelectionModel().getSelectedItem() != null) {
            Tab selected = tabs.getSelectionModel().getSelectedItem();
            Node content = selected.getContent();
            if (content != null) {
                for (Node n : content.lookupAll(".button")) {
                    if (n instanceof Button b && text.equals(b.getText())) return b;
                }
            }
        }
        for (Node n : primaryStage.getScene().getRoot().lookupAll(".button")) {
            if (n instanceof Button b && text.equals(b.getText())) return b;
        }
        return null;
    }
    /** Verifies mo login shows applicants tab. */
    @Test
    @DisplayName("MO login lands on Applicants tab and renders welcome label")
    void moLoginShowsApplicantsTab() {
        loginAsBob();
        Label welcome = fx("#welcomeLabel");
        assertEquals("Welcome, Bob", welcome.getText());
        TabPane tabs = fx("#tabPane");
        assertEquals("Applicants", tabs.getSelectionModel().getSelectedItem().getText());
    }
    /** Verifies admin tabs only for admin. */
    @Test
    @DisplayName("Admin tabs only appear after admin login")
    void adminTabsOnlyForAdmin() {
        loginAsBob();
        TabPane tabs = fx("#tabPane");
        for (Tab t : tabs.getTabs()) {
            assertFalse("TA admin".equals(t.getText()), "MO should not see TA admin tab");
        }
        loginAsAdmin();
        TabPane tabs2 = fx("#tabPane");
        boolean hasTaAdmin = tabs2.getTabs().stream().anyMatch(t -> "TA admin".equals(t.getText()));
        assertTrue(hasTaAdmin, "Admin should see TA admin tab");
    }
    /** Verifies profile save updates mo. */
    @Test
    @DisplayName("Profile tab shows Bob's data; saving persists changes")
    void profileSaveUpdatesMo() {
        loginAsBob();
        selectTab("My profile");
        runOnFx(() -> {
            assertEquals("mo.bob@bupt.edu.cn", ((TextField) fx("#moEmailField")).getText());
            ((TextField) fx("#moFullNameField")).setText("Bob Senior");
            ((TextField) fx("#moPhoneField")).setText("13888888888");
            ((TextArea) fx("#moModuleArea")).setText("Math, Physics");
            findButtonByText("Save profile").fire();
        });
        try { Thread.sleep(150); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        WaitForAsyncUtils.waitForFxEvents();
        Mo stored = services.profileService().findMo("mo.bob@bupt.edu.cn").orElseThrow();
        assertEquals("Bob Senior", stored.getFullName());
        assertEquals("13888888888", stored.getPhone());
        assertEquals("Math, Physics", stored.getResponsibleModules());
    }
    /** Verifies my jobs tab lists jobs. */
    @Test
    @DisplayName("My jobs tab lists Bob's jobs")
    void myJobsTabListsJobs() {
        loginAsBob();
        selectTab("My jobs");
        TableView<Job> table = fx("#myJobTable");
        assertEquals(1, table.getItems().size());
    }
    /** Verifies my jobs search filter. */
    @Test
    @DisplayName("Search filter narrows the My jobs list")
    void myJobsSearchFilter() {
        addJob("Beta Job", "Beta", 1, "r", "n", LocalDate.now(), LocalDate.now().plusDays(15));
        loginAsBob();
        selectTab("My jobs");
        runOnFx(() -> {
            ((TextField) fx("#myJobSearchField")).setText("Beta");
            findButtonByText("Refresh").fire();
        });
        WaitForAsyncUtils.waitForFxEvents();
        TableView<Job> table = fx("#myJobTable");
        assertEquals(1, table.getItems().size());
        assertEquals("Beta Job", table.getItems().get(0).getJobName());
    }
    /** Verifies post job requires fields. */
    @Test
    @DisplayName("Post / edit job: missing required fields trigger error")
    void postJobRequiresFields() {
        loginAsBob();
        selectTab("Post / edit job");
        runOnFx(() -> {
            // Click Save with empty form.
            ((TextField) fx("#jobNameField")).setText("");
            ((TextField) fx("#moduleField")).setText("");
            findButtonByText("Save").fire();
        });
        try { Thread.sleep(150); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        WaitForAsyncUtils.waitForFxEvents();
        // No new job was saved (still 1 from seed).
        assertEquals(1, services.jobService().findJobsByMo("mo.bob@bupt.edu.cn").size());
    }
    /** Verifies post job saves when valid. */
    @Test
    @DisplayName("Post / edit job: saving valid form creates a new job")
    void postJobSavesWhenValid() {
        loginAsBob();
        selectTab("Post / edit job");
        runOnFx(() -> {
            ((TextField) fx("#jobNameField")).setText("Brand New Job");
            ((TextField) fx("#moduleField")).setText("Brand");
            ((Spinner<Integer>) fx("#positionsSpinner")).getValueFactory().setValue(3);
            ((DatePicker) fx("#startDatePicker")).setValue(LocalDate.now());
            ((DatePicker) fx("#endDatePicker")).setValue(LocalDate.now().plusDays(10));
            ((TextArea) fx("#requirementsField")).setText("Be smart");
            ((TextArea) fx("#notesField")).setText("Friendly team");
            findButtonByText("Save").fire();
        });
        WaitForAsyncUtils.waitForFxEvents();
        List<Job> jobs = services.jobService().findJobsByMo("mo.bob@bupt.edu.cn");
        assertTrue(jobs.stream().anyMatch(j -> "Brand New Job".equals(j.getJobName())));
    }
    /** Verifies clear resets form. */
    @Test
    @DisplayName("Clear button resets form to 'New job'")
    void clearResetsForm() {
        loginAsBob();
        selectTab("Post / edit job");
        runOnFx(() -> {
            ((TextField) fx("#jobNameField")).setText("DRAFT");
            findButtonByText("Clear").fire();
        });
        WaitForAsyncUtils.waitForFxEvents();
        assertEquals("", ((TextField) fx("#jobNameField")).getText());
        assertEquals("New job", ((Label) fx("#formJobIdLabel")).getText());
    }
    /** Verifies edit button populates form. */
    @Test
    @DisplayName("Edit button populates the form with selected job's data")
    void editButtonPopulatesForm() {
        loginAsBob();
        selectTab("My jobs");
        runOnFx(() -> {
            TableView<Job> table = fx("#myJobTable");
            table.getSelectionModel().selectFirst();
            findButtonByText("Edit").fire();
        });
        WaitForAsyncUtils.waitForFxEvents();
        // Should switch to Post/edit and load job into form.
        TabPane tabs = fx("#tabPane");
        assertEquals("Post / edit job", tabs.getSelectionModel().getSelectedItem().getText());
        assertEquals("Initial Job", ((TextField) fx("#jobNameField")).getText());
        assertEquals("InitialMod", ((TextField) fx("#moduleField")).getText());
    }
    /** Verifies close job logs action. */
    @Test
    @DisplayName("Closing a job updates DAO state and JobLog")
    void closeJobLogsAction() {
        loginAsBob();
        selectTab("My jobs");
        nextAlertResponse = ButtonBar.ButtonData.OK_DONE;
        runOnFx(() -> {
            TableView<Job> table = fx("#myJobTable");
            table.getSelectionModel().selectFirst();
            findButtonByText("Close").fire();
        });
        try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        WaitForAsyncUtils.waitForFxEvents();
        Job job = services.jobService().findJobsByMo("mo.bob@bupt.edu.cn").get(0);
        assertFalse(job.isOpen());
        assertFalse(services.jobLogDao().findAll().isEmpty(), "Close action should produce a job log");
    }
    /** Verifies no applicants state. */
    @Test
    @DisplayName("Job selector cycles to the open job and 'No applicants for this job yet.' shows when empty")
    void noApplicantsState() {
        loginAsBob();
        selectTab("Applicants");
        ComboBox<Job> selector = fx("#jobSelector");
        assertNotNull(selector);
        assertEquals(1, selector.getItems().size());
        FlowPane cards = fx("#applicantCardPane");
        assertNotNull(cards);
        Label empty = fx("#applicantsEmptyLabel");
        assertTrue(empty.isVisible());
        assertEquals("No applicants for this job yet.", empty.getText());
    }
    /** Verifies hire applicant success. */
    @Test
    @DisplayName("Hire button hires selected applicant and refreshes UI")
    void hireApplicantSuccess() {
        // Apply Daniel to Bob's initial job.
        Job job = services.jobService().findJobsByMo("mo.bob@bupt.edu.cn").get(0);
        services.applicationService().applyForJob("ta.daniel@bupt.edu.cn", job);

        loginAsBob();
        selectTab("Applicants");
        // Auto-confirm the "Hire this applicant?" dialog.
        nextAlertResponse = ButtonBar.ButtonData.OK_DONE;
        runOnFx(() -> {
            // The applicant card is rendered automatically by the controller.
            Button hire = fx("#hireButton");
            assertNotNull(hire);
            assertFalse(hire.isDisable(), "Hire button should be enabled when a pending applicant is selected");
            hire.fire();
        });
        // Allow time for dialog auto-dismiss + state update.
        try { Thread.sleep(400); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        WaitForAsyncUtils.waitForFxEvents();
        // Service should reflect the hire.
        long hired = services.applicationService().findByJob(job.getJobId()).stream()
                .filter(r -> r.getStatus() == ApplicationStatus.HIRED)
                .count();
        assertEquals(1, hired);
    }
    /** Verifies reject applicant success. */
    @Test
    @DisplayName("Reject button rejects selected applicant")
    void rejectApplicantSuccess() {
        Job job = services.jobService().findJobsByMo("mo.bob@bupt.edu.cn").get(0);
        services.applicationService().applyForJob("ta.daniel@bupt.edu.cn", job);
        loginAsBob();
        selectTab("Applicants");
        nextAlertResponse = ButtonBar.ButtonData.OK_DONE;
        runOnFx(() -> {
            Button reject = fx("#rejectButton");
            assertNotNull(reject);
            assertFalse(reject.isDisable());
            reject.fire();
        });
        try { Thread.sleep(300); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        WaitForAsyncUtils.waitForFxEvents();
        long rejected = services.applicationService().findByJob(job.getJobId()).stream()
                .filter(r -> r.getStatus() == ApplicationStatus.REJECTED)
                .count();
        assertEquals(1, rejected);
    }
    /** Verifies logout navigates to login. */
    @Test
    @DisplayName("Logout returns to login view")
    void logoutNavigatesToLogin() {
        loginAsBob();
        runOnFx(() -> findButtonByText("Log out").fire());
        WaitForAsyncUtils.waitForFxEvents();
        assertNotNull(fx("#registerLink"));
    }
    /** Verifies admin ta toggle status. */
    @Test
    @DisplayName("Admin: TA admin tab populates and toggles status")
    void adminTaToggleStatus() {
        loginAsAdmin();
        selectTab("TA admin");
        TableView<AdminTaDisplay> table = fx("#adminTaTable");
        assertNotNull(table);
        assertTrue(table.getItems().size() >= 2,
                "Admin should see Alice and Daniel TA accounts");
        runOnFx(() -> {
            for (int i = 0; i < table.getItems().size(); i++) {
                if ("ta.daniel@bupt.edu.cn".equalsIgnoreCase(
                        table.getItems().get(i).getTa().getTaId())) {
                    table.getSelectionModel().select(i);
                    break;
                }
            }
            findButtonByText("Enable / disable").fire();
        });
        try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        WaitForAsyncUtils.waitForFxEvents();
        Ta toggled = services.profileService().findTa("ta.daniel@bupt.edu.cn").orElseThrow();
        assertTrue(toggled.isDisabled(), "Daniel should be disabled after toggling status");
    }
    /** Verifies admin mo reset password. */
    @Test
    @DisplayName("Admin: MO admin tab populates and resets MO password")
    void adminMoResetPassword() {
        loginAsAdmin();
        selectTab("MO admin");
        TableView<AdminMoDisplay> table = fx("#adminMoTable");
        assertNotNull(table);
        // Bob shows up; admin filtered out.
        boolean bobPresent = table.getItems().stream()
                .anyMatch(d -> "mo.bob@bupt.edu.cn".equalsIgnoreCase(d.getMo().getMoId()));
        assertTrue(bobPresent);
        runOnFx(() -> {
            for (int i = 0; i < table.getItems().size(); i++) {
                if ("mo.bob@bupt.edu.cn".equalsIgnoreCase(
                        table.getItems().get(i).getMo().getMoId())) {
                    table.getSelectionModel().select(i);
                    break;
                }
            }
            findButtonByText("Reset password").fire();
        });
        try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        WaitForAsyncUtils.waitForFxEvents();
        // Reset password sets default password.
        Mo bob = services.profileService().findMo("mo.bob@bupt.edu.cn").orElseThrow();
        assertFalse("pass1234".equals(bob.getPassword()),
                "Reset password should change Bob's password from the seeded one");
    }
    /** Verifies admin all jobs close action. */
    @Test
    @DisplayName("Admin: All Jobs tab lists every job and supports admin close")
    void adminAllJobsCloseAction() {
        loginAsAdmin();
        selectTab("All jobs");
        TableView<AdminJobDisplay> table = fx("#adminJobTable");
        assertNotNull(table);
        assertFalse(table.getItems().isEmpty());
        nextAlertResponse = ButtonBar.ButtonData.OK_DONE;
        runOnFx(() -> {
            table.getSelectionModel().selectFirst();
            findButtonByText("Close Job").fire();
        });
        try { Thread.sleep(300); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        WaitForAsyncUtils.waitForFxEvents();
        // Job log entry should appear.
        assertFalse(services.jobLogDao().findAll().isEmpty(),
                "Admin closing a job should add a job log entry");
    }
    /** Verifies admin job management shows logs. */
    @Test
    @DisplayName("Admin: Job management tab refreshes job logs after closing a job")
    void adminJobManagementShowsLogs() {
        // Pre-populate logs by closing the seed job via service + log directly.
        Job initial = services.jobService().findJobsByMo("mo.bob@bupt.edu.cn").get(0);
        var log = new com.bupt.tarecruit.entity.JobLog();
        log.setLogId("jlog-test-1");
        log.setAdminId("admin@bupt.edu.cn");
        log.setJobId(initial.getJobId());
        log.setAction(com.bupt.tarecruit.entity.JobLog.JobLogAction.CLOSE_JOB);
        log.setPreviousState("Open");
        log.setNewState("Closed");
        log.setTimestamp(java.time.LocalDateTime.now());
        services.jobLogDao().save(log);

        loginAsAdmin();
        selectTab("Job Management Log");
        TableView<?> jobLogTable = fx("#jobLogTable");
        assertNotNull(jobLogTable);
        assertFalse(jobLogTable.getItems().isEmpty());
    }
    /** Verifies admin account management shows logs. */
    @Test
    @DisplayName("Admin: Account management tab lists account logs")
    void adminAccountManagementShowsLogs() {
        // Generate a log by toggling Daniel's status.
        services.adminService().toggleStatus(Role.TA, "ta.daniel@bupt.edu.cn", true, "admin@bupt.edu.cn");
        loginAsAdmin();
        selectTab("Account Management Log");
        TableView<?> accountLogTable = fx("#accountLogTable");
        assertNotNull(accountLogTable);
        assertFalse(accountLogTable.getItems().isEmpty());
    }
    /** Verifies admin insights loads once. */
    @Test
    @DisplayName("Admin: Insights tab loads metrics on first visit")
    void adminInsightsLoadsOnce() {
        loginAsAdmin();
        selectTab("Insights");
        try { Thread.sleep(300); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        WaitForAsyncUtils.waitForFxEvents();
        Label totalJobs = fx("#totalJobsLabel");
        assertNotNull(totalJobs);
        // 1 seeded job; non-zero unless data didn't propagate.
        assertEquals("1", totalJobs.getText());
    }

    /** Holds a reference so {@link ApplicationRecord} stays linked in test Javadoc. */
    @SuppressWarnings("unused")
    private ApplicationRecord referenced;
}
