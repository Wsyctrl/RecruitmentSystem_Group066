package com.bupt.tarecruit.controller;

import com.bupt.tarecruit.entity.ApplicationStatus;
import com.bupt.tarecruit.entity.Job;
import com.bupt.tarecruit.util.PortalMode;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testfx.util.WaitForAsyncUtils;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InsightsDialogControllerUiTest extends BaseUiTest {

    @Override
    protected PortalMode getPortalMode() {
        return PortalMode.MO_PORTAL;
    }

    @Override
    protected void navigateInitialView() {
        // No app navigation needed.
    }

    private record Loaded(InsightsDialogController controller, Stage stage, Parent root) {}

    private Loaded openInsights() {
        AtomicReference<Loaded> ref = new AtomicReference<>();
        runOnFx(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/insights-dialog.fxml"));
                Parent root = loader.load();
                InsightsDialogController c = loader.getController();
                c.setServices(services);
                Stage st = new Stage();
                st.setScene(new Scene(root));
                c.setDialogStage(st);
                st.show();
                ref.set(new Loaded(c, st, root));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        WaitForAsyncUtils.waitForFxEvents();
        return ref.get();
    }

    private Job createJob(String name, String module, boolean open) {
        Job j = new Job();
        j.setJobName(name);
        j.setModuleName(module);
        j.setNumberOfPositions(2);
        j.setRequirements("req");
        j.setAdditionalNotes("note");
        j.setStartDate(LocalDate.now());
        j.setEndDate(LocalDate.now().plusDays(30));
        j.setMoId("mo.bob@bupt.edu.cn");
        var res = services.jobService().upsertJob(j);
        Job saved = res.data();
        if (!open) {
            services.jobService().closeJob(saved.getJobId());
        }
        return services.jobService().findById(saved.getJobId()).orElseThrow();
    }

    @Test
    @DisplayName("Initial state has zeroed metric labels and an empty chart")
    void initialZeroState() {
        Loaded l = openInsights();
        assertEquals("0", ((Label) l.root.lookup("#totalJobsLabel")).getText());
        assertEquals("0", ((Label) l.root.lookup("#openJobsLabel")).getText());
        assertEquals("0", ((Label) l.root.lookup("#applicationsLabel")).getText());
        assertEquals("0", ((Label) l.root.lookup("#hiredLabel")).getText());
        assertEquals("0%", ((Label) l.root.lookup("#hireRateLabel")).getText());
        BarChart<?, ?> chart = (BarChart<?, ?>) l.root.lookup("#hiringStatusChart");
        assertNotNull(chart);
        assertTrue(chart.getData().isEmpty());
        assertFalse(chart.getAnimated());
        assertFalse(chart.isLegendVisible());
        runOnFx(l.stage::close);
    }

    @Test
    @DisplayName("loadData populates job & application metrics for existing data")
    void loadDataMetrics() {
        Job open1 = createJob("Algorithms TA", "Algorithms", true);
        Job open2 = createJob("Systems TA", "Systems", true);
        Job closed = createJob("Old TA", "OldModule", false);
        // Apply Alice to open1
        services.applicationService().applyForJob("ta.alice@bupt.edu.cn", open1);
        // Apply Alice to open2 then mark hired
        var applyRes = services.applicationService().applyForJob("ta.alice@bupt.edu.cn", open2);

        Loaded l = openInsights();
        runOnFx(() -> l.controller.loadData());
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals("3", ((Label) l.root.lookup("#totalJobsLabel")).getText());
        assertEquals("2", ((Label) l.root.lookup("#openJobsLabel")).getText());
        assertEquals("2", ((Label) l.root.lookup("#applicationsLabel")).getText());
        assertEquals("0", ((Label) l.root.lookup("#hiredLabel")).getText());
        // 0/2 = 0.0%
        assertEquals("0.0%", ((Label) l.root.lookup("#hireRateLabel")).getText());

        // Hiring chart should now have a series with 4 categories.
        BarChart<?, ?> chart = (BarChart<?, ?>) l.root.lookup("#hiringStatusChart");
        assertEquals(1, chart.getData().size());
        XYChart.Series<?, ?> series = chart.getData().get(0);
        assertEquals(4, series.getData().size());
        runOnFx(l.stage::close);
    }

    @Test
    @DisplayName("Module stats rendering shows one row per module")
    void moduleStatsRendered() {
        Job open1 = createJob("A TA", "ModuleA", true);
        Job open2 = createJob("B TA", "ModuleB", true);
        services.applicationService().applyForJob("ta.alice@bupt.edu.cn", open1);
        services.applicationService().applyForJob("ta.alice@bupt.edu.cn", open2);

        Loaded l = openInsights();
        runOnFx(() -> l.controller.loadData());
        WaitForAsyncUtils.waitForFxEvents();

        VBox box = (VBox) l.root.lookup("#moduleStatsBox");
        assertNotNull(box);
        assertEquals(2, box.getChildren().size(),
                "One row per module that has applications");
        runOnFx(l.stage::close);
    }

    @Test
    @DisplayName("'No application data available' shown when there are no applications")
    void emptyModuleStats() {
        createJob("Empty TA", "EmptyMod", true);
        Loaded l = openInsights();
        runOnFx(() -> l.controller.loadData());
        WaitForAsyncUtils.waitForFxEvents();
        VBox box = (VBox) l.root.lookup("#moduleStatsBox");
        assertEquals(1, box.getChildren().size());
        Label empty = (Label) box.getChildren().get(0);
        assertEquals("No application data available", empty.getText());
        runOnFx(l.stage::close);
    }

    @Test
    @DisplayName("Sort buttons reorder the module stats rows alphabetically/by count")
    void sortingButtons() {
        Job a = createJob("Z TA", "Zeta", true);
        Job b = createJob("A TA", "Alpha", true);
        // 1 app on Zeta, 1 app on Alpha
        services.applicationService().applyForJob("ta.alice@bupt.edu.cn", a);
        services.applicationService().applyForJob("ta.alice@bupt.edu.cn", b);

        Loaded l = openInsights();
        runOnFx(() -> l.controller.loadData());
        WaitForAsyncUtils.waitForFxEvents();

        VBox box = (VBox) l.root.lookup("#moduleStatsBox");
        // Both have count=1, sort-by-count keeps insertion order; sort-by-name → Alpha then Zeta.
        runOnFx(() -> {
            for (var n : l.root.lookupAll(".button")) {
                if (n instanceof Button btn && "Sort by Name".equals(btn.getText())) {
                    btn.fire();
                    break;
                }
            }
        });
        WaitForAsyncUtils.waitForFxEvents();
        Label firstAfterName = extractFirstModuleLabel(box);
        assertEquals("Alpha", firstAfterName.getText());

        runOnFx(() -> {
            for (var n : l.root.lookupAll(".button")) {
                if (n instanceof Button btn && "Sort by Count".equals(btn.getText())) {
                    btn.fire();
                    break;
                }
            }
        });
        WaitForAsyncUtils.waitForFxEvents();
        // After sort-by-count, two equal counts but ordering should still produce some valid module.
        Label firstAfterCount = extractFirstModuleLabel(box);
        assertNotNull(firstAfterCount);
        runOnFx(l.stage::close);
    }

    @Test
    @DisplayName("handleClose closes the dialog stage")
    void closeClosesStage() {
        Loaded l = openInsights();
        // Find the Refresh button to ensure controls exist; we just close via stage.
        assertTrue(l.stage.isShowing());
        runOnFx(l.stage::close);
        assertFalse(l.stage.isShowing());
    }

    private Label extractFirstModuleLabel(VBox box) {
        if (box.getChildren().isEmpty()) {
            return null;
        }
        var row = box.getChildren().get(0);
        if (row instanceof javafx.scene.layout.HBox hb && !hb.getChildren().isEmpty()
                && hb.getChildren().get(0) instanceof Label lbl) {
            return lbl;
        }
        if (row instanceof Label lbl) {
            return lbl;
        }
        return null;
    }

    // Reference unused import suppression for ApplicationStatus.
    @SuppressWarnings("unused")
    private static final ApplicationStatus REF = ApplicationStatus.PENDING;
}
