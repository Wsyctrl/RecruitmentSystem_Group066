package com.bupt.tarecruit.controller;

import com.bupt.tarecruit.entity.UserSession;
import com.bupt.tarecruit.util.DialogUtil;
import com.bupt.tarecruit.util.OperationResult;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.util.Duration;

/**
 * Controller for the login view.
 * Handles user login submission and navigation to the registration page.
 */
public class LoginController extends BaseController {

    /**
     * Input field for the user ID.
     */
    @FXML
    private TextField userIdField;

    /**
     * Input field for the password.
     */
    @FXML
    private PasswordField passwordField;

    /**
     * Link that navigates the user to the registration page.
     */
    @FXML
    private Hyperlink registerLink;
    @FXML
    private Button backButton;
    @FXML
    private Label noticeLabel;
    @FXML
    private HBox noticeBox;

    /**
     * Initializes the login view after the FXML components are loaded.
     * Binds the register link to the registration page navigation action.
     */
    @FXML
    private void initialize() {
        registerLink.setOnAction(e -> navigator.showRegister());
        if (noticeBox != null) {
            noticeBox.setVisible(false);
            noticeBox.setManaged(false);
        }
        if (backButton != null) {
            backButton.setVisible(false);
            backButton.setManaged(false);
        }
    }

    public void configureForPortal(boolean showBackButton, String noticeMessage) {
        if (backButton != null) {
            backButton.setVisible(showBackButton);
            backButton.setManaged(showBackButton);
        }
        if (noticeMessage != null && !noticeMessage.isBlank()) {
            showNotice(noticeMessage);
        }
    }

    @FXML
    private void handleBackToBrowse() {
        navigator.showTaGuestDashboard();
    }

    /**
     * Handles the login action triggered from the UI.
     */
    @FXML
    private void handleLogin() {
        try {
            OperationResult<UserSession> result = services.authService().login(userIdField.getText(), passwordField.getText());
            if (result.success()) {
                UserSession session = result.data();
                boolean taPortal = navigator.isTaPortal();
                boolean taUser = session.role() == com.bupt.tarecruit.entity.Role.TA;
                if (taPortal && !taUser) {
                    DialogUtil.error("MO/Admin accounts must sign in from the MO portal.", navigator.getPrimaryStage());
                    return;
                }
                if (!taPortal && taUser) {
                    DialogUtil.error("TA accounts must sign in from the TA portal.", navigator.getPrimaryStage());
                    return;
                }
                navigator.showDashboard(session);
            } else {
                DialogUtil.error(result.message(), navigator.getPrimaryStage());
            }
        } catch (IllegalArgumentException ex) {
            DialogUtil.error(ex.getMessage(), navigator.getPrimaryStage());
        }
    }

    private void showNotice(String message) {
        if (noticeLabel == null || noticeBox == null) {
            return;
        }
        noticeLabel.setText(message);
        noticeBox.setVisible(true);
        noticeBox.setManaged(true);
        PauseTransition pause = new PauseTransition(Duration.seconds(2.5));
        pause.setOnFinished(event -> {
            noticeBox.setVisible(false);
            noticeBox.setManaged(false);
        });
        pause.play();
    }
}