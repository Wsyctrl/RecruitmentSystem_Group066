package com.bupt.tarecruit.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.Optional;

/**
 * Factory helpers for JavaFX alert dialogs with consistent English button labels.
 * <p>
 * Uses explicit English button types so labels remain correct on non-English OS locales.
 * </p>
 */
public final class DialogUtil {

    /** Maximum width applied to alert message labels for readable line wrapping. */
    private static final double MESSAGE_MAX_WIDTH = 520;
    /** Explicit English labels; default ButtonType.OK is localized on non-English OS locales. */
    private static final ButtonType OK_EN = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
    private static final ButtonType CANCEL_EN = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
    private static final ButtonType YES_EN = new ButtonType("Yes", ButtonBar.ButtonData.YES);
    private static final ButtonType NO_EN = new ButtonType("No", ButtonBar.ButtonData.NO);

    private DialogUtil() {
    }

    /**
     * Shows an information dialog and waits until the user dismisses it.
     *
     * @param content message body text
     * @param owner   optional owner window for modality; may be {@code null}
     */
    public static void info(String content, Window owner) {
        createAlert(Alert.AlertType.INFORMATION, "Information", content, owner).showAndWait();
    }

    /**
     * Shows an error dialog and waits until the user dismisses it.
     *
     * @param content message body text
     * @param owner   optional owner window for modality; may be {@code null}
     */
    public static void error(String content, Window owner) {
        createAlert(Alert.AlertType.ERROR, "Error", content, owner).showAndWait();
    }

    /**
     * Shows a confirmation dialog with OK and Cancel buttons.
     *
     * @param content message body text
     * @param owner   optional owner window for modality; may be {@code null}
     * @return {@code true} when the user chooses OK; {@code false} otherwise
     */
    public static boolean confirm(String content, Window owner) {
        Optional<ButtonType> result = createAlert(Alert.AlertType.CONFIRMATION, "Confirm", content, owner).showAndWait();
        return result.map(bt -> bt.getButtonData() == ButtonBar.ButtonData.OK_DONE).orElse(false);
    }

    /**
     * Shows a confirmation dialog with Yes and No buttons.
     *
     * @param content message body text
     * @param owner   optional owner window for modality; may be {@code null}
     * @return {@code true} when the user chooses Yes; {@code false} otherwise
     */
    public static boolean confirmYesNo(String content, Window owner) {
        Alert alert = createAlert(Alert.AlertType.CONFIRMATION, "Confirm", content, owner);
        alert.getButtonTypes().setAll(YES_EN, NO_EN);
        Optional<ButtonType> result = alert.showAndWait();
        return result.map(bt -> bt.getButtonData() == ButtonBar.ButtonData.YES).orElse(false);
    }

    /**
     * Asks whether to recommend similar pending candidates after a hire decision.
     *
     * @param applicantLabel display name of the reference applicant; may be blank
     * @param owner          optional owner window for modality; may be {@code null}
     * @return the chosen count (1–10) when the user selects Yes; empty when No or closed
     */
    public static Optional<Integer> confirmRecommendSimilarCandidates(String applicantLabel, Window owner) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Recommend Similar Candidates");
        alert.setHeaderText(null);
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.getButtonTypes().setAll(YES_EN, NO_EN);

        Label message = new Label("""
                Would you like AI to recommend similar candidates based on this applicant's profile?""");
        message.setWrapText(true);
        message.setMaxWidth(MESSAGE_MAX_WIDTH);

        Label nameLabel = new Label(applicantLabel == null || applicantLabel.isBlank()
                ? "Reference applicant: (selected)"
                : "Reference applicant: " + applicantLabel);
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(MESSAGE_MAX_WIDTH);

        Label countLabel = new Label("Number of similar candidates to recommend (1–10):");
        Spinner<Integer> countSpinner = new Spinner<>();
        countSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 3));
        countSpinner.setEditable(true);
        countSpinner.setPrefWidth(80);

        VBox content = new VBox(10, message, nameLabel, countLabel, countSpinner);
        content.setMinWidth(MESSAGE_MAX_WIDTH);
        alert.getDialogPane().setContent(content);
        alert.getDialogPane().setMinWidth(MESSAGE_MAX_WIDTH + 80);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get().getButtonData() != ButtonBar.ButtonData.YES) {
            return Optional.empty();
        }
        Integer value = countSpinner.getValue();
        if (value == null) {
            return Optional.of(3);
        }
        return Optional.of(Math.max(1, Math.min(10, value)));
    }

    /**
     * Builds an alert with wrapped message text and English button labels.
     *
     * @param type    alert severity/type
     * @param title   dialog title
     * @param content message body text
     * @param owner   optional owner window; may be {@code null}
     * @return configured alert (not yet shown)
     */
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
