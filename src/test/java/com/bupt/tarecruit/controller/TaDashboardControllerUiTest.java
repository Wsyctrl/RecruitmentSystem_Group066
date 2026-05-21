package com.bupt.tarecruit.controller;

import com.bupt.tarecruit.entity.Job;
import com.bupt.tarecruit.entity.Role;
import com.bupt.tarecruit.entity.UserSession;
import com.bupt.tarecruit.util.PortalMode;
import com.bupt.tarecruit.viewmodel.ApplicationDisplay;
import com.bupt.tarecruit.viewmodel.TaJobDisplay;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testfx.util.WaitForAsyncUtils;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaDashboardControllerUiTest extends BaseUiTest {

    @Override
    protected PortalMode getPortalMode() {
        return PortalMode.TA_PORTAL;
    }

    @Override
    protected void navigateInitialView() {
        // Most tests will set their own scene; default to login.
        navigator.showLogin();
    }

    @Override
    protected void seedData() {
        super.seedData();
        // Seed a few open jobs from Bob.
        addJob("Algorithms Aide", "Algorithms", 2, "Java", "Bonus", LocalDate.now(), LocalDate.now().plusDays(30));
        addJob("Networking TA", "Networks", 1, "Routing", "", LocalDate.now(), LocalDate.now().plusDays(20));
        addJob("Old Position", "OldMod", 1, "Old", "", LocalDate.now().minusDays(20), LocalDate.now().minusDays(1));
    }

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

    private void loginAsAlice() {
        runOnFx(() -> {
            UserSession session = new UserSession(Role.TA,
                    services.profileService().findTa("ta.alice@bupt.edu.cn").orElseThrow(),
                    null);
            navigator.showDashboard(session);
        });
        WaitForAsyncUtils.waitForFxEvents();
    }

    private void enterGuestMode() {
        runOnFx(() -> navigator.showTaGuestDashboard());
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    @DisplayName("Guest mode populates welcome label and switches to Browse jobs tab")
    void guestModeBasics() {
        enterGuestMode();
        Label welcome = fx("#welcomeLabel");
        assertEquals("Browse jobs as guest", welcome.getText());
        Button auth = fx("#authButton");
        assertEquals("Log in", auth.getText());
        TabPane tabs = fx("#tabPane");
        assertEquals("Browse jobs", tabs.getSelectionModel().getSelectedItem().getText());
    }

    @Test
    @DisplayName("Guest mode shows open jobs only")
    void guestModeShowsOpenJobsOnly() {
        enterGuestMode();
        TableView<TaJobDisplay> table = fx("#jobTable");
        assertNotNull(table);
        // 2 open jobs, 1 closed-but-not-yet-normalized: the bootstrap calls normalizePendingApplicationsForClosedJobs;
        // it doesn't close already-open jobs. Our seed only creates open jobs, but "Old Position" has an end date
        // in the past — still open by status. So the open-jobs filter shows all 3.
        assertEquals(3, table.getItems().size());
    }

    @Test
    @DisplayName("Authenticated TA sees welcome label with full name and lands on My applications")
    void loginShowsTaDashboard() {
        loginAsAlice();
        Label welcome = fx("#welcomeLabel");
        assertEquals("Welcome, Alice", welcome.getText());
        Button auth = fx("#authButton");
        assertEquals("Log out", auth.getText());
        TabPane tabs = fx("#tabPane");
        assertEquals("My applications", tabs.getSelectionModel().getSelectedItem().getText());
    }

    @Test
    @DisplayName("Profile tab loads Alice's profile fields after login")
    void profileTabPopulatesFromSession() {
        loginAsAlice();
        runOnFx(() -> {
            TabPane tabs = fx("#tabPane");
            for (Tab t : tabs.getTabs()) {
                if ("My profile".equals(t.getText())) {
                    tabs.getSelectionModel().select(t);
                    break;
                }
            }
        });
        WaitForAsyncUtils.waitForFxEvents();
        TextField email = fx("#emailField");
        TextField fullName = fx("#fullNameField");
        assertEquals("ta.alice@bupt.edu.cn", email.getText());
        assertEquals("Alice", fullName.getText());
        assertFalse(email.isEditable(), "Email field is read-only");
        // CV state.
        Label cvPath = fx("#cvPathLabel");
        assertEquals("No file uploaded", cvPath.getText());
        Button download = fx("#downloadCvButton");
        Button delete = fx("#deleteCvButton");
        Button aiFill = fx("#aiFillProfileButton");
        assertTrue(download.isDisable());
        assertTrue(delete.isDisable());
        assertTrue(aiFill.isDisable());
    }

    @Test
    @DisplayName("Save profile persists field changes")
    void saveProfileWritesChanges() {
        loginAsAlice();
        runOnFx(() -> {
            TabPane tabs = fx("#tabPane");
            for (Tab t : tabs.getTabs()) {
                if ("My profile".equals(t.getText())) {
                    tabs.getSelectionModel().select(t);
                    break;
                }
            }
        });
        WaitForAsyncUtils.waitForFxEvents();
        runOnFx(() -> {
            ((TextField) fx("#fullNameField")).setText("Alice Updated");
            ((TextField) fx("#phoneField")).setText("13900000000");
            ((TextField) fx("#majorField")).setText("Math");
            ((TextArea) fx("#skillsArea")).setText("Python");
            Button saveBtn = findButtonByText("Save profile");
            assertNotNull(saveBtn);
            saveBtn.fire();
        });
        WaitForAsyncUtils.waitForFxEvents();
        var stored = services.profileService().findTa("ta.alice@bupt.edu.cn").orElseThrow();
        assertEquals("Alice Updated", stored.getFullName());
        assertEquals("13900000000", stored.getPhone());
        assertEquals("Math", stored.getMajor());
        assertEquals("Python", stored.getSkills());
    }

    @Test
    @DisplayName("Browse Jobs tab loads open job list and shows details for the first job")
    void browseJobsShowsDetails() {
        loginAsAlice();
        runOnFx(() -> {
            TabPane tabs = fx("#tabPane");
            for (Tab t : tabs.getTabs()) {
                if ("Browse jobs".equals(t.getText())) {
                    tabs.getSelectionModel().select(t);
                    break;
                }
            }
        });
        WaitForAsyncUtils.waitForFxEvents();
        TableView<TaJobDisplay> table = fx("#jobTable");
        assertNotNull(table);
        assertEquals(3, table.getItems().size());
        // Selecting a job populates the detail panel.
        runOnFx(() -> table.getSelectionModel().selectFirst());
        WaitForAsyncUtils.waitForFxEvents();
        Label name = fx("#jobNameLabel");
        assertNotNull(name);
        assertFalse(name.getText().isBlank());
        Button apply = fx("#applyButton");
        assertNotNull(apply);
        assertFalse(apply.isDisable(), "Apply enabled for open un-applied job");
    }

    @Test
    @DisplayName("Search filter limits jobs to keyword match after Refresh button is pressed")
    void searchFiltersJobs() {
        loginAsAlice();
        runOnFx(() -> {
            TabPane tabs = fx("#tabPane");
            for (Tab t : tabs.getTabs()) {
                if ("Browse jobs".equals(t.getText())) {
                    tabs.getSelectionModel().select(t);
                    break;
                }
            }
        });
        WaitForAsyncUtils.waitForFxEvents();
        runOnFx(() -> {
            ((TextField) fx("#jobSearchField")).setText("Network");
            findButtonByText("Refresh").fire();
        });
        WaitForAsyncUtils.waitForFxEvents();
        TableView<TaJobDisplay> table = fx("#jobTable");
        assertEquals(1, table.getItems().size());
        assertEquals("Networking TA", table.getItems().get(0).getJobName());
    }

    @Test
    @DisplayName("Apply button submits an application that appears under My applications")
    void applyForAJob() {
        loginAsAlice();
        runOnFx(() -> {
            TabPane tabs = fx("#tabPane");
            for (Tab t : tabs.getTabs()) {
                if ("Browse jobs".equals(t.getText())) {
                    tabs.getSelectionModel().select(t);
                    break;
                }
            }
        });
        WaitForAsyncUtils.waitForFxEvents();
        runOnFx(() -> {
            TableView<TaJobDisplay> table = fx("#jobTable");
            table.getSelectionModel().selectFirst();
            Button apply = fx("#applyButton");
            apply.fire();
        });
        WaitForAsyncUtils.waitForFxEvents();
        // After apply, application should be in DAO and on the My applications tab.
        var apps = services.applicationService().findActiveApplicationsForTa("ta.alice@bupt.edu.cn");
        assertEquals(1, apps.size());
        // Apply button is now disabled (Already Applied)
        Button apply = fx("#applyButton");
        assertTrue(apply.isDisable());
        assertEquals("Already Applied", apply.getText());
    }

    @Test
    @DisplayName("Withdrawing a pending application reverts the apply button")
    void withdrawApplication() {
        loginAsAlice();
        // Pre-apply via service directly.
        Job target = services.jobService().findOpenJobs().get(0);
        services.applicationService().applyForJob("ta.alice@bupt.edu.cn", target);
        runOnFx(() -> {
            TabPane tabs = fx("#tabPane");
            for (Tab t : tabs.getTabs()) {
                if ("My applications".equals(t.getText())) {
                    tabs.getSelectionModel().select(t);
                    break;
                }
            }
        });
        WaitForAsyncUtils.waitForFxEvents();
        runOnFx(() -> {
            TableView<ApplicationDisplay> table = fx("#applicationTable");
            // Refresh state since session was set before service apply.
            // The controller loaded data on setSession; we already applied earlier so reload.
            // Easiest: navigate again to trigger setSession reload.
        });
        // Re-navigate to refresh
        runOnFx(() -> {
            UserSession session = new UserSession(Role.TA,
                    services.profileService().findTa("ta.alice@bupt.edu.cn").orElseThrow(),
                    null);
            navigator.showDashboard(session);
        });
        WaitForAsyncUtils.waitForFxEvents();
        // Select first application row and click Withdraw.
        nextAlertResponse = javafx.scene.control.ButtonBar.ButtonData.OK_DONE;
        runOnFx(() -> {
            TableView<ApplicationDisplay> table = fx("#applicationTable");
            assertEquals(1, table.getItems().size());
            table.getSelectionModel().selectFirst();
            Button withdraw = fx("#withdrawButton");
            withdraw.fire();
        });
        try { Thread.sleep(150); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        WaitForAsyncUtils.waitForFxEvents();
        // After withdraw, no active applications.
        assertEquals(0,
                services.applicationService().findActiveApplicationsForTa("ta.alice@bupt.edu.cn").size());
    }

    @Test
    @DisplayName("Guest mode redirects to login when clicking apply with a selected job")
    void guestApplyRedirectsToLogin() {
        enterGuestMode();
        runOnFx(() -> {
            TableView<TaJobDisplay> table = fx("#jobTable");
            table.getSelectionModel().selectFirst();
            Button apply = fx("#applyButton");
            apply.fire();
        });
        WaitForAsyncUtils.waitForFxEvents();
        // Guest applies → redirected to login (registerLink present only on login view).
        assertNotNull(fx("#registerLink"));
    }

    @Test
    @DisplayName("Log out button returns to login (MO portal) / guest browse (TA portal)")
    void logOutFromTaPortalReturnsToGuestDashboard() {
        loginAsAlice();
        runOnFx(() -> {
            Button auth = fx("#authButton");
            auth.fire();
        });
        WaitForAsyncUtils.waitForFxEvents();
        Label welcome = fx("#welcomeLabel");
        assertEquals("Browse jobs as guest", welcome.getText(),
                "TA portal: logging out returns to guest browse dashboard");
    }

    @Test
    @DisplayName("Guest selecting empty job table clears detail panel")
    void detailPanelEmptyState() {
        // Force no jobs by closing all.
        for (Job j : services.jobService().findAllJobs()) {
            services.jobService().closeJob(j.getJobId());
        }
        enterGuestMode();
        Label name = fx("#jobNameLabel");
        assertEquals("Select a job", name.getText());
        Button apply = fx("#applyButton");
        assertTrue(apply.isDisable());
        assertEquals("Apply", apply.getText());
    }

    private Button findButtonByText(String text) {
        for (Node n : primaryStage.getScene().getRoot().lookupAll(".button")) {
            if (n instanceof Button b && text.equals(b.getText())) return b;
        }
        return null;
    }
}
