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
 * Handles JavaFX scene transitions and application navigation for a single portal instance.
 * <p>
 * Reuses one {@link Scene} on the primary stage so size and styles persist across views.
 * Controllers implementing {@link BaseController} receive the navigator and service registry on load.
 * </p>
 */
public class SceneNavigator {

    /** Default width for newly created scenes when no prior scene exists. */
    private static final double DEFAULT_SCENE_WIDTH = 1100;

    /** Default height for newly created scenes when no prior scene exists. */
    private static final double DEFAULT_SCENE_HEIGHT = 720;

    /**
     * Main application window.
     */
    private final Stage primaryStage;

    /**
     * Application service registry passed to controllers.
     */
    private final ServiceRegistry services;

    /**
     * Whether this instance serves the TA or MO portal.
     */
    private final PortalMode portalMode;

    /**
     * Currently logged-in user session; {@code null} on login/register/guest views.
     */
    private UserSession currentSession;

    /**
     * Reused scene so width and height follow the stage; avoids tiny pref-sized scenes
     * stuck top-left after {@link Stage#setScene(Scene)}.
     */
    private Scene sharedScene;

    /**
     * Creates a navigator for the given stage, services, and portal mode.
     *
     * @param primaryStage main window stage
     * @param services     application service registry
     * @param portalMode   TA or MO portal configuration
     */
    public SceneNavigator(Stage primaryStage, ServiceRegistry services, PortalMode portalMode) {
        this.primaryStage = primaryStage;
        this.services = services;
        this.portalMode = portalMode;
    }

    /**
     * Returns the primary stage.
     *
     * @return main window stage
     */
    public Stage getPrimaryStage() {
        return primaryStage;
    }

    /**
     * Returns the service registry.
     *
     * @return application services
     */
    public ServiceRegistry getServices() {
        return services;
    }

    /**
     * Returns the active user session, or {@code null} when not logged in.
     *
     * @return current session, if any
     */
    public UserSession getCurrentSession() {
        return currentSession;
    }

    /**
     * Returns whether this navigator serves the teaching assistant portal.
     *
     * @return {@code true} for {@link PortalMode#TA_PORTAL}
     */
    public boolean isTaPortal() {
        return portalMode == PortalMode.TA_PORTAL;
    }

    /**
     * Returns whether login and register screens should show a back button.
     * Only the TA portal shows a back control to the guest dashboard.
     *
     * @return {@code true} on the TA portal
     */
    public boolean shouldShowAuthBackButton() {
        return isTaPortal();
    }

    /**
     * Returns the default role pre-selected on the registration screen.
     *
     * @return {@link Role#TA} for the TA portal, {@link Role#MO} for the MO portal
     */
    public Role getRegisterDefaultRole() {
        return isTaPortal() ? Role.TA : Role.MO;
    }

    /**
     * Navigates to the login view and clears the current session.
     */
    public void showLogin() {
        currentSession = null;
        loadScene("login-view.fxml", controller -> {
            if (controller instanceof LoginController loginController) {
                loginController.configureForPortal(shouldShowAuthBackButton(), null);
            }
        });
    }

    /**
     * Navigates to the login view with an optional notice message.
     *
     * @param notice message shown on the login screen; may be {@code null}
     */
    public void showLoginWithNotice(String notice) {
        currentSession = null;
        loadScene("login-view.fxml", controller -> {
            if (controller instanceof LoginController loginController) {
                loginController.configureForPortal(shouldShowAuthBackButton(), notice);
            }
        });
    }

    /**
     * Navigates to the registration view with portal-appropriate defaults.
     */
    public void showRegister() {
        loadScene("register-view.fxml", controller -> {
            if (controller instanceof RegisterController registerController) {
                registerController.configureForPortal(getRegisterDefaultRole(), shouldShowAuthBackButton());
            }
        });
    }

    /**
     * Navigates to the TA dashboard in guest (unauthenticated) mode.
     */
    public void showTaGuestDashboard() {
        currentSession = null;
        loadScene("ta-dashboard-view.fxml", controller -> {
            if (controller instanceof TaDashboardController taDashboardController) {
                taDashboardController.enterGuestMode();
            }
        });
    }

    /**
     * Navigates to the role-appropriate dashboard for the given session.
     *
     * @param session authenticated user session
     */
    public void showDashboard(UserSession session) {
        this.currentSession = session;
        if (session.role() == Role.TA) {
            loadScene("ta-dashboard-view.fxml", controller -> applySession(controller, session));
        } else {
            loadScene("mo-dashboard-view.fxml", controller -> applySession(controller, session));
        }
    }

    /**
     * Applies the session to controllers that implement {@link SessionAware}.
     *
     * @param controller loaded FXML controller instance
     * @param session    user session to attach
     */
    private void applySession(Object controller, UserSession session) {
        if (controller instanceof SessionAware aware) {
            aware.setSession(session);
        }
    }

    /**
     * Loads an FXML view, initializes the controller, and swaps it into the shared scene.
     *
     * @param fxml               FXML file name under {@code /fxml/}
     * @param controllerConsumer optional callback after controller initialization
     */
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

    /**
     * Applies BootstrapFX and application CSS to a scene.
     *
     * @param scene scene to style
     */
    private void applyStyles(Scene scene) {
        scene.getStylesheets().add(BootstrapFX.bootstrapFXStylesheet());
        var customCss = getClass().getResource("/css/application.css");
        if (Objects.nonNull(customCss)) {
            scene.getStylesheets().add(customCss.toExternalForm());
        }
    }
}
