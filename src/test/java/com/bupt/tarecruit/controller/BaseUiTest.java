package com.bupt.tarecruit.controller;

import com.bupt.tarecruit.entity.Mo;
import com.bupt.tarecruit.entity.Ta;
import com.bupt.tarecruit.service.ServiceRegistry;
import com.bupt.tarecruit.util.PortalMode;
import com.bupt.tarecruit.util.SceneNavigator;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.testfx.api.FxRobot;
import org.testfx.api.FxToolkit;
import org.testfx.util.WaitForAsyncUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Base class for TestFX-based controller UI tests.
 * Each test gets its own temp data directory, ServiceRegistry, and freshly
 * navigated scene. Auto-dismisses Alert dialogs with a configurable button.
 */
public abstract class BaseUiTest extends FxRobot {

    protected Path dataDir;
    protected ServiceRegistry services;
    protected SceneNavigator navigator;
    protected Stage primaryStage;

    /** Which button data to press on the NEXT alert that appears. */
    protected volatile ButtonBar.ButtonData nextAlertResponse = ButtonBar.ButtonData.OK_DONE;
    private ListChangeListener<Window> windowListener;

    @BeforeAll
    static void initToolkit() throws Exception {
        System.setProperty("testfx.robot", "glass");
        System.setProperty("prism.order", "sw");
        System.setProperty("prism.text", "t2k");
        FxToolkit.registerPrimaryStage();
    }

    @AfterAll
    static void shutdownToolkit() throws Exception {
        FxToolkit.cleanupStages();
    }

    @BeforeEach
    void setUpUi() throws Exception {
        dataDir = Files.createTempDirectory("tarecruit-ui-test-");
        services = new ServiceRegistry(dataDir);
        seedData();

        primaryStage = FxToolkit.registerPrimaryStage();
        runOnFx(() -> {
            navigator = new SceneNavigator(primaryStage, services, getPortalMode());
            installDialogAutoCloser();
            primaryStage.setWidth(1280);
            primaryStage.setHeight(800);
            navigateInitialView();
            if (!primaryStage.isShowing()) {
                primaryStage.show();
            }
            primaryStage.toFront();
        });
        WaitForAsyncUtils.waitForFxEvents();
    }

    @AfterEach
    void tearDownUi() throws Exception {
        uninstallDialogAutoCloser();
        runOnFx(() -> {
            // Close any leftover dialogs before tearing down stage.
            for (Window w : Window.getWindows().toArray(new Window[0])) {
                if (w instanceof Stage st && st != primaryStage) {
                    st.close();
                }
            }
        });
        WaitForAsyncUtils.waitForFxEvents();
        runOnFx(() -> {
            if (primaryStage != null && primaryStage.getScene() != null) {
                primaryStage.getScene().setRoot(new javafx.scene.layout.StackPane());
            }
        });
        WaitForAsyncUtils.waitForFxEvents();
    }

    /** Default seed: minimal users. Override to extend. */
    protected void seedData() {
        // Register + update fields. AuthService.register creates the row; updateTa fills profile.
        services.authService().register(com.bupt.tarecruit.entity.Role.TA,
                "ta.alice@bupt.edu.cn", "pass1234", "pass1234");
        services.profileService().findTa("ta.alice@bupt.edu.cn").ifPresent(ta -> {
            ta.setFullName("Alice");
            ta.setMajor("CS");
            ta.setSkills("Java");
            ta.setExperience("2y");
            ta.setSelfEvaluation("Diligent");
            services.profileService().updateTa(ta);
        });

        services.authService().register(com.bupt.tarecruit.entity.Role.MO,
                "mo.bob@bupt.edu.cn", "pass1234", "pass1234");
        services.profileService().findMo("mo.bob@bupt.edu.cn").ifPresent(mo -> {
            mo.setFullName("Bob");
            mo.setPhone("12300000000");
            mo.setResponsibleModules("Math");
            services.profileService().updateMo(mo);
        });

        services.authService().register(com.bupt.tarecruit.entity.Role.MO,
                "admin@bupt.edu.cn", "admin1234", "admin1234");
        services.profileService().findMo("admin@bupt.edu.cn").ifPresent(admin -> {
            admin.setFullName("Admin");
            services.profileService().updateMo(admin);
        });
    }

    /** Subclasses tell us which portal to load. */
    protected abstract PortalMode getPortalMode();

    /** Subclasses route to a starting view. Default to login. */
    protected void navigateInitialView() {
        navigator.showLogin();
    }

    /** Run code on FX thread, blocking until done. */
    protected void runOnFx(Runnable r) {
        if (Platform.isFxApplicationThread()) {
            r.run();
            return;
        }
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> err = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                r.run();
            } catch (Throwable t) {
                err.set(t);
            } finally {
                latch.countDown();
            }
        });
        try {
            if (!latch.await(15, TimeUnit.SECONDS)) {
                throw new RuntimeException("FX runnable timed out");
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(ie);
        }
        if (err.get() != null) {
            throw new RuntimeException(err.get());
        }
    }

    /** Convenient FX-thread lookup for a node by id (#id). */
    @SuppressWarnings("unchecked")
    protected <T extends Node> T fx(String selector) {
        Scene scene = primaryStage.getScene();
        if (scene == null) {
            return null;
        }
        return (T) scene.lookup(selector);
    }

    /** Auto-dismiss any Alert/Dialog that opens, using nextAlertResponse. */
    private void installDialogAutoCloser() {
        windowListener = change -> {
            while (change.next()) {
                for (Window w : change.getAddedSubList()) {
                    if (!(w instanceof Stage stage)) {
                        continue;
                    }
                    stage.showingProperty().addListener((obs, was, is) -> {
                        if (is) {
                            tryDismissDialog(stage);
                        }
                    });
                    if (stage.isShowing()) {
                        tryDismissDialog(stage);
                    }
                }
            }
        };
        Window.getWindows().addListener(windowListener);
    }

    private void uninstallDialogAutoCloser() {
        if (windowListener != null) {
            try {
                Window.getWindows().removeListener(windowListener);
            } catch (Exception ignored) {
            }
            windowListener = null;
        }
    }

    private void tryDismissDialog(Stage stage) {
        Scene scene = stage.getScene();
        if (scene == null) {
            return;
        }
        Node root = scene.getRoot();
        if (!(root instanceof DialogPane dp)) {
            return;
        }
        ButtonBar.ButtonData desired = nextAlertResponse;
        Platform.runLater(() -> {
            Button target = null;
            for (ButtonType bt : dp.getButtonTypes()) {
                if (bt.getButtonData() == desired) {
                    Object node = dp.lookupButton(bt);
                    if (node instanceof Button btn) {
                        target = btn;
                        break;
                    }
                }
            }
            if (target == null && !dp.getButtonTypes().isEmpty()) {
                Object node = dp.lookupButton(dp.getButtonTypes().get(0));
                if (node instanceof Button btn) {
                    target = btn;
                }
            }
            if (target != null) {
                target.fire();
            } else {
                stage.close();
            }
            // Reset to default for next dialog.
            nextAlertResponse = ButtonBar.ButtonData.OK_DONE;
        });
    }
}
