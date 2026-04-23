package com.bupt.tarecruit.util;

import com.bupt.tarecruit.controller.BaseController;
import com.bupt.tarecruit.controller.LoginController;
import com.bupt.tarecruit.controller.RegisterController;
import com.bupt.tarecruit.controller.SessionAware;
import com.bupt.tarecruit.controller.TaDashboardController;
import com.bupt.tarecruit.entity.Role;
import com.bupt.tarecruit.entity.UserSession;
import com.bupt.tarecruit.service.ServiceRegistry;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.kordamp.bootstrapfx.BootstrapFX;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Handles scene transitions and basic application navigation.
 */
public class SceneNavigator {

    private static final double DEFAULT_SCENE_WIDTH = 1100;
    private static final double DEFAULT_SCENE_HEIGHT = 720;

    private final Stage primaryStage;
    private final ServiceRegistry services;
    private final PortalMode portalMode;
    private UserSession currentSession;
    /** Reused so width/height follow the stage; avoids tiny pref-sized scenes stuck top-left after setScene. */
    private Scene sharedScene;

    public SceneNavigator(Stage primaryStage, ServiceRegistry services, PortalMode portalMode) {
        this.primaryStage = primaryStage;
        this.services = services;
        this.portalMode = portalMode;
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public ServiceRegistry getServices() {
        return services;
    }

    public UserSession getCurrentSession() {
        return currentSession;
    }

    public boolean isTaPortal() {
        return portalMode == PortalMode.TA_PORTAL;
    }

    public boolean shouldShowAuthBackButton() {
        return isTaPortal();
    }

    public Role getRegisterDefaultRole() {
        return isTaPortal() ? Role.TA : Role.MO;
    }

    public void showLogin() {
        currentSession = null;
        loadScene("login-view.fxml", controller -> {
            if (controller instanceof LoginController loginController) {
                loginController.configureForPortal(shouldShowAuthBackButton(), null);
            }
        });
    }

    public void showLoginWithNotice(String notice) {
        currentSession = null;
        loadScene("login-view.fxml", controller -> {
            if (controller instanceof LoginController loginController) {
                loginController.configureForPortal(shouldShowAuthBackButton(), notice);
            }
        });
    }

    public void showRegister() {
        loadScene("register-view.fxml", controller -> {
            if (controller instanceof RegisterController registerController) {
                registerController.configureForPortal(getRegisterDefaultRole(), shouldShowAuthBackButton());
            }
        });
    }

    public void showTaGuestDashboard() {
        currentSession = null;
        loadScene("ta-dashboard-view.fxml", controller -> {
            if (controller instanceof TaDashboardController taDashboardController) {
                taDashboardController.enterGuestMode();
            }
        });
    }

    public void showDashboard(UserSession session) {
        this.currentSession = session;
        if (session.role() == Role.TA) {
            loadScene("ta-dashboard-view.fxml", controller -> applySession(controller, session));
        } else {
            loadScene("mo-dashboard-view.fxml", controller -> applySession(controller, session));
        }
    }

    private void applySession(Object controller, UserSession session) {
        if (controller instanceof SessionAware aware) {
            aware.setSession(session);
        }
    }

    private void loadScene(String fxml, Consumer<Object> controllerConsumer) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/" + fxml));
            Parent root = loader.load();
            Object controller = loader.getController();
            if (controller instanceof BaseController baseController) {
                baseController.init(this, services);
            }
            if (controllerConsumer != null) {
                controllerConsumer.accept(controller);
            }
            if (sharedScene == null) {
                double w = primaryStage.getWidth();
                double h = primaryStage.getHeight();
                if (w <= 0 || Double.isNaN(w)) {
                    w = DEFAULT_SCENE_WIDTH;
                }
                if (h <= 0 || Double.isNaN(h)) {
                    h = DEFAULT_SCENE_HEIGHT;
                }
                sharedScene = new Scene(root, w, h);
                applyStyles(sharedScene);
                primaryStage.setScene(sharedScene);
            } else {
                sharedScene.setRoot(root);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load view " + fxml, e);
        }
    }

    private void applyStyles(Scene scene) {
        scene.getStylesheets().add(BootstrapFX.bootstrapFXStylesheet());
        var customCss = getClass().getResource("/css/application.css");
        if (Objects.nonNull(customCss)) {
            scene.getStylesheets().add(customCss.toExternalForm());
        }
    }
}
