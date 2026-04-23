package com.bupt.tarecruit.controller;

import com.bupt.tarecruit.entity.Role;
import com.bupt.tarecruit.util.DialogUtil;
import com.bupt.tarecruit.util.OperationResult;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Controller for the registration view.
 * Handles user registration input, role selection, and navigation back to the login page.
 */
public class RegisterController extends BaseController {

    /**
     * Choice box used to select the user role during registration.
     */
    @FXML
    private ChoiceBox<Role> roleChoice;
    @FXML
    private Label registerRoleHintLabel;
    @FXML
    private Button backButton;
    private Role fixedRole = Role.TA;

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
     * Input field for confirming the password.
     */
    @FXML
    private PasswordField confirmPasswordField;

    /**
     * Initializes the registration view after the FXML components are loaded.
     * Sets the available roles and the default selected role.
     */
    @FXML
    private void initialize() {
        if (backButton != null) {
            backButton.setVisible(false);
            backButton.setManaged(false);
        }
    }

    public void configureForPortal(Role defaultRole, boolean showBackButton) {
        this.fixedRole = defaultRole == null ? Role.TA : defaultRole;
        if (roleChoice != null) {
            roleChoice.setVisible(false);
            roleChoice.setManaged(false);
            roleChoice.setValue(this.fixedRole);
        }
        if (registerRoleHintLabel != null) {
            registerRoleHintLabel.setText(this.fixedRole == Role.TA ? "Register as TA" : "Register as MO");
        }
        if (backButton != null) {
            backButton.setVisible(showBackButton);
            backButton.setManaged(showBackButton);
        }
    }

    /**
     * Handles the registration action triggered from the UI.
     * Validates required input fields before calling the authentication service.
     */
    @FXML
    private void handleRegister() {
        Role role = fixedRole == null ? Role.TA : fixedRole;
        String userId = userIdField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (userId == null || userId.trim().isEmpty()) {
            DialogUtil.error("Email cannot be empty.", navigator.getPrimaryStage());
            return;
        }

        if (password == null || password.trim().isEmpty()) {
            DialogUtil.error("Password cannot be empty.", navigator.getPrimaryStage());
            return;
        }

        if (confirmPassword == null || confirmPassword.trim().isEmpty()) {
            DialogUtil.error("Please confirm your password.", navigator.getPrimaryStage());
            return;
        }

        OperationResult<Void> result = services.authService()
                .register(role, userId, password, confirmPassword);

        if (result.success()) {
            DialogUtil.info(result.message(), navigator.getPrimaryStage());
            navigator.showLogin();
        } else {
            DialogUtil.error(result.message(), navigator.getPrimaryStage());
        }
    }

    /**
     * Navigates the user back to the login page.
     */
    @FXML
    private void handleBackToLogin() {
        navigator.showLogin();
    }

    @FXML
    private void handleBackToBrowse() {
        navigator.showTaGuestDashboard();
    }
}