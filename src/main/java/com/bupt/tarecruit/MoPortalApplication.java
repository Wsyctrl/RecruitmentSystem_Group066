package com.bupt.tarecruit;

import com.bupt.tarecruit.service.ServiceRegistry;
import com.bupt.tarecruit.util.PortalMode;
import com.bupt.tarecruit.util.SceneNavigator;
import javafx.application.Application;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.Locale;

/**
 * JavaFX entry point for the Module Organizer (MO) and admin portal.
 * Initializes CSV-backed services, forces English locale, and opens the login screen.
 */
public class MoPortalApplication extends Application {

    /**
     * Configures the primary stage, wires {@link ServiceRegistry} and {@link SceneNavigator}
     * in {@link PortalMode#MO_PORTAL}, and shows the login view.
     *
     * @param stage primary window provided by JavaFX
     */
    @Override
    public void start(Stage stage) {
        Path dataDir = Path.of("data");
        ServiceRegistry serviceRegistry = new ServiceRegistry(dataDir);
        SceneNavigator navigator = new SceneNavigator(stage, serviceRegistry, PortalMode.MO_PORTAL);

        stage.setTitle("BUPT International School TA Recruitment");
        stage.setMinWidth(1100);
        stage.setMinHeight(720);

        navigator.showLogin();
        stage.show();
    }

    /**
     * Sets default locale categories to English and launches the JavaFX application.
     *
     * @param args command-line arguments forwarded to {@link Application#launch(String...)}
     */
    public static void main(String[] args) {
        Locale english = Locale.ENGLISH;
        Locale.setDefault(english);
        Locale.setDefault(Locale.Category.DISPLAY, english);
        Locale.setDefault(Locale.Category.FORMAT, english);
        launch(args);
    }

    /**
     * IDE-friendly launcher that delegates to {@link #main(String[])} for packaged MO builds.
     */
    public static class Launcher {
        /**
         * @param args command-line arguments
         */
        public static void main(String[] args) {
            MoPortalApplication.main(args);
        }
    }
}
