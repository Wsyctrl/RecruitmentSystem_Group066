package com.bupt.tarecruit;

import com.bupt.tarecruit.service.ServiceRegistry;
import com.bupt.tarecruit.util.SceneNavigator;
import javafx.application.Application;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.Locale;

/**
 * Main JavaFX application bootstrapper.
 */
public class TaRecruitApplication extends Application {

    @Override
    public void start(Stage stage) {
        Path dataDir = Path.of("data");
        ServiceRegistry serviceRegistry = new ServiceRegistry(dataDir);
        SceneNavigator navigator = new SceneNavigator(stage, serviceRegistry);

        stage.setTitle("BUPT International School TA Recruitment");
        stage.setMinWidth(1100);
        stage.setMinHeight(720);

        navigator.showLogin();
        stage.show();
    }

    public static void main(String[] args) {
        Locale english = Locale.ENGLISH;
        Locale.setDefault(english);
        Locale.setDefault(Locale.Category.DISPLAY, english);
        Locale.setDefault(Locale.Category.FORMAT, english);
        launch(args);
    }
}
