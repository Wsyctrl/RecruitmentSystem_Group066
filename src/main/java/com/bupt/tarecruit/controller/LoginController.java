package com.bupt.tarecruit.controller;

import com.bupt.tarecruit.entity.UserSession;
import com.bupt.tarecruit.util.DialogUtil;
import com.bupt.tarecruit.util.OperationResult;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

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

    /**
     * Initializes the login view after the FXML components are loaded.
     * Binds the register link to the registration page navigation action.
     */
    @FXML
    private void initialize() {
        registerLink.setOnAction(e -> navigator.showRegister());
    }

    /**
     * Handles the login action triggered from the UI.
     * Validates required input fields before calling the authentication service.
     */
    @FXML
    private void handleLogin() {
        String userId = userIdField.getText();
        String password = passwordField.getText();

        if (userId == null || userId.trim().isEmpty()) {
            DialogUtil.error("User ID cannot be empty.", navigator.getPrimaryStage());
            return;
        }

        if (password == null || password.trim().isEmpty()) {
            DialogUtil.error("Password cannot be empty.", navigator.getPrimaryStage());
            return;
        }

        try {
            OperationResult<UserSession> result = services.authService().login(userId, password);
            if (result.success()) {
                navigator.showDashboard(result.data());
            } else {
                DialogUtil.error(result.message(), navigator.getPrimaryStage());
            }
        } catch (IllegalArgumentException ex) {
            DialogUtil.error(ex.getMessage(), navigator.getPrimaryStage());
        }
    }
}