package com.bupt.tarecruit.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.stage.Window;

import java.util.Optional;

public final class DialogUtil {

    private static final double MESSAGE_MAX_WIDTH = 520;
    /** Explicit English labels; default ButtonType.OK is localized on non-English OS locales. */
    private static final ButtonType OK_EN = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
    private static final ButtonType CANCEL_EN = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

    private DialogUtil() {
    }

    public static void info(String content, Window owner) {
        createAlert(Alert.AlertType.INFORMATION, "Information", content, owner).showAndWait();
    }

    public static void error(String content, Window owner) {
        createAlert(Alert.AlertType.ERROR, "Error", content, owner).showAndWait();
    }

    public static boolean confirm(String content, Window owner) {
        Optional<ButtonType> result = createAlert(Alert.AlertType.CONFIRMATION, "Confirm", content, owner).showAndWait();
        return result.map(bt -> bt.getButtonData() == ButtonBar.ButtonData.OK_DONE).orElse(false);
    }

    private static Alert createAlert(Alert.AlertType type, String title, String content, Window owner) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        Label label = new Label(content);
        label.setWrapText(true);
        label.setMaxWidth(MESSAGE_MAX_WIDTH);
        label.setMinHeight(Region.USE_PREF_SIZE);
        alert.getDialogPane().setContent(label);
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.getDialogPane().setMinWidth(MESSAGE_MAX_WIDTH + 80);
        if (type == Alert.AlertType.CONFIRMATION) {
            alert.getButtonTypes().setAll(OK_EN, CANCEL_EN);
        } else {
            alert.getButtonTypes().setAll(OK_EN);
        }
        return alert;
    }
}
